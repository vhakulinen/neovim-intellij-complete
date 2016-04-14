import com.google.common.net.HostAndPort;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.util.ui.UIUtil;
import com.neovim.*;
import com.neovim.msgpack.MessagePackRPC;
import complete.DeopleteHelper;
import complete.DeopleteItem;
import complete.EmbeditorRequestHandler;

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

        public Updater(Neovim nvim){
            mNeovim = nvim;
            mEmbeditorRequestHandler = new EmbeditorRequestHandler();
        }

        @NeovimHandler("TextChanged")
        public void changed(String args) {
            LOG.info("Text changed");
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
    }

    public NeovimIntellijComplete() {
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        MessagePackRPC.Connection conn;
        HostAndPort hp = HostAndPort.fromParts("127.0.0.1", 7650);
        try {
            conn = new SocketNeovim(hp);
        } catch (IOException ex) {
            LOG.error("Failed to connect to neovim", ex);
            return;

        }
        mNeovim = Neovim.connectTo(conn);
        LOG.info("Connected to neovim");

        long cid = mNeovim.getChannelId().join();
        mNeovim.commandOutput("let g:intellijID=" + cid);
        mNeovim.register(new Updater(mNeovim));

        mNeovim.sendVimCommand("echo 'Intellij connected.'");
    }

}