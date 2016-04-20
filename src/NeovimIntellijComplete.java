import codeinspect.Inspect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.codeInsight.CodeSmellInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.util.ui.UIUtil;
import com.neovim.Neovim;
import com.neovim.NeovimHandler;
import com.neovim.SocketNeovim;
import com.neovim.msgpack.MessagePackRPC;
import complete.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NeovimIntellijComplete extends AnAction {

    private static final Logger LOG = Logger.getInstance(NeovimIntellijComplete.class);

    private Neovim mNeovim;

    public static class Updater {
        private static final Logger LOG = Logger.getInstance(NeovimIntellijComplete.class);

        private Neovim mNeovim;
        private EmbeditorRequestHandler mEmbeditorRequestHandler;
        private Fix[] mCachedFixes = new Fix[0];

        public Updater(Neovim nvim){
            mNeovim = nvim;
            mEmbeditorRequestHandler = new EmbeditorRequestHandler();
        }

        /**
         * Hack to have up to date files when doing quickfix stuff...
         * @param path
         */
        @NeovimHandler("IntellijOnWrite")
        public void intellijOnWrite(String path) {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                PsiFile f = EmbeditorUtil.findTargetFile(path);
                if (f != null) {
                    VirtualFile vf = f.getVirtualFile();
                    if (vf != null)
                        vf.refresh(false, true);
                }
            }, ModalityState.any());
        }

        @NeovimHandler("TextChanged")
        public void changed(String args) {
            LOG.info("Text changed");
        }

        @NeovimHandler("IntellijFixProblem")
        public void intellijFixProblem(String path, List<String> lines, int fixId) {
            final String fileContent = String.join("\n", lines) ;
            for (Fix f : mCachedFixes) {
                if (f.getFixId() == fixId) {
                    Inspect.doFix(path, fileContent, f.getAction());
                    break;
                }
            }

        }

        @NeovimHandler("IntellijProblems")
        public Fix[] intellijProblems(String path, List<String> lines, final int row, final int col) {
            final String fileContent = String.join("\n", lines) ;
            List<HighlightInfo.IntentionActionDescriptor> allFixes = Inspect.getFixes(path, fileContent, row, col);
            List<Fix> fixes = new ArrayList<>();
            for (int i = 0; i < allFixes.size(); i++) {
                HighlightInfo.IntentionActionDescriptor d = allFixes.get(i);
                if (d.getAction().getText().length() == 0) continue;
                fixes.add(new Fix(d.getAction().getText(), i, d));
            }
            mCachedFixes = fixes.toArray(new Fix[fixes.size()]);
            return mCachedFixes;
        }

        @NeovimHandler("IntellijCodeSmell")
        public Problem[] intellijCodeSmell(final String path, final List<String> lines) {
            final String fileContent = String.join("\n", lines);
            List<Problem> retval = new ArrayList<Problem>();
            CodeSmellInfo[] smells = mEmbeditorRequestHandler.inspectCode(path, fileContent);
            for (CodeSmellInfo smell : smells) {
                retval.add(new Problem(smell.getStartLine(), smell.getStartColumn(), smell.getDescription()));
            }
            return retval.toArray(new Problem[]{});
        }

        @NeovimHandler("IntellijComplete")
        public DeopleteItem[] intellijComplete(final String path, final String bufferContents,
                                               final int row, final int col) {

            LookupElement[] c = mEmbeditorRequestHandler.getCompletionVariants(path, bufferContents, row, col);
            if (c.length < 0) return null;
            DeopleteHelper dh = new DeopleteHelper();
                UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
                        for (LookupElement i : c) {
                            if (i instanceof PsiPackage
                                    || i instanceof LookupElementBuilder
                                    || i.getPsiElement() instanceof PsiPackageImpl) {
                                dh.add(i.getLookupString(), "", "");
                                continue;
                            }
                            String word = i.getLookupString();
                            List<String> params = new ArrayList<String>();
                            String info;
                            String kind = "";
                            PsiElement psiElement = i.getPsiElement();
                            if (psiElement == null) {
                                dh.add(word, "", "");
                                continue;
                            }
                            for (PsiElement e : psiElement.getChildren()) {
                                if (e instanceof PsiParameterList) {
                                    for (PsiParameter param : ((PsiParameterList)e).getParameters()) {
                                        params.add(param.getTypeElement().getText() + " " + param.getName());
                                    }
                                } else if (e instanceof PsiTypeElement) {
                                    kind = e.getText();
                                }
                            }

                            info = "(" + String.join(", ", params) + ")";
                            dh.add(word, info, kind);
                        }
                });
            return dh.getItems();
        }

        private class Fix {
            @JsonProperty
            private String description;
            @JsonProperty
            private int fixId;

            @JsonIgnore
            private HighlightInfo.IntentionActionDescriptor action;

            public Fix(String description, int fixId, HighlightInfo.IntentionActionDescriptor action) {
                this.description = description;
                this.fixId = fixId;
                this.action = action;
            }

            public String getDescription() {
                return description;
            }

            public int getFixId() {
                return fixId;
            }

            public HighlightInfo.IntentionActionDescriptor getAction() {
                return action;
            }
        }
    }

    public NeovimIntellijComplete() {
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        NeovimDialog dialog = new NeovimDialog(true);
        dialog.show();

        if (dialog.isOK()) {
            LOG.warn(dialog.getAddr());

            MessagePackRPC.Connection conn;
            //HostAndPort hp = HostAndPort.fromParts("127.0.0.1", 7650);
            try {
                conn = new SocketNeovim(dialog.getAddr());
            } catch (IOException ex) {
                LOG.error("Failed to connect to neovim", ex);
                return;
            }
            mNeovim = Neovim.connectTo(conn);
            LOG.info("Connected to neovim");

            long cid = mNeovim.getChannelId().join();
            mNeovim.commandOutput("let g:intellijID=" + cid);
            // Refresh file on intellij on write so we can have uptodate stuff when doing codeanalyzis
            mNeovim.commandOutput("au BufWritePost * call rpcnotify(g:intellijID, \"IntellijOnWrite\", expand(\"%:p\"))");
            mNeovim.register(new Updater(mNeovim));

            mNeovim.sendVimCommand("echo 'Intellij connected.'");
        }

    }
}