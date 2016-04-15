package complete;

import com.intellij.codeInsight.CodeSmellInfo;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CodeSmellDetector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class EmbeditorRequestHandler {
    private final static Logger LOG = Logger.getInstance(EmbeditorRequestHandler.class);

    public Hashtable[] resolve(String path, String fileContent, int line, int column) {
        LOG.debug(String.format("resolve(%s:%d:%d)", path, line, column));
        List<ResolveOutcome> resolveOutcomes = EmbeditorUtil.getResolveOutcomes(path, fileContent, line, column);
        Hashtable[] results = new Hashtable[resolveOutcomes.size()];
        for (int i = 0; i < resolveOutcomes.size(); i++) {
            ResolveOutcome resolveOutcome = resolveOutcomes.get(i);
            Hashtable<String, Object> result = new Hashtable<String, Object>();
            result.put("path", resolveOutcome.getFilePath());
            result.put("line", resolveOutcome.getRow());
            result.put("column", resolveOutcome.getColumn());
            result.put("text", resolveOutcome.getText());
            results[i] = result;
        }
        return results;
    }

    public int getCompletionStartOffsetInLine(String path, String fileContent, int line, int column) {
        LOG.debug(String.format("getCompletionStartOffsetInLine(%s:%d:%d)", path, line, column));
        final Ref<Integer> integerRef = Ref.create(0);
        EmbeditorUtil.performCompletion(path, fileContent, line, column, new EmbeditorUtil.CompletionCallback() {
            @Override
            public void completionFinished(@NotNull CompletionParameters parameters,
                                           @NotNull CompletionProgressIndicator indicator,
                                           @NotNull Document document) {
                integerRef.set(EmbeditorUtil.getOffsetFromLineStart(parameters, document));
            }
        });
        return integerRef.get();
    }

    public LookupElement[] getCompletionVariants(String path, String fileContent, int line, int column) {
        LOG.debug(String.format("getCompletionVariants(%s:%d:%d)", path, line, column));
        final Collection<LookupElement> completionVariants = ContainerUtil.newLinkedList();
        EmbeditorUtil.performCompletion(path, fileContent, line, column, new EmbeditorUtil.CompletionCallback() {
            @Override
            public void completionFinished(@NotNull CompletionParameters parameters,
                                           @NotNull CompletionProgressIndicator indicator,
                                           @NotNull Document document) {
                for (LookupElement item : indicator.getLookup().getItems()) {
                    //completionVariants.add(item.getUserData(key).toString().replace("\u0000###", "").replace("###", ""));
                    completionVariants.add(item);
                }
            }
        });
        return completionVariants.toArray(new LookupElement[completionVariants.size()]);
    }

    public CodeSmellInfo[] inspectCode(final String path, String fileContent) {
        final CodeSmellInfo[][] resultsWrapper = new CodeSmellInfo[1][];
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                final PsiFile targetPsiFile = EmbeditorUtil.findTargetFile(path);
                Project project = targetPsiFile.getProject();
                if (targetPsiFile != null) {
                    List<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();
                    virtualFiles.add(EmbeditorUtil.createDummyVirtualFile(project, fileContent, targetPsiFile));
                    List<CodeSmellInfo> problems = CodeSmellDetector.getInstance(project).findCodeSmells(virtualFiles);
                    resultsWrapper[0] = problems.toArray(new CodeSmellInfo[problems.size()]);
                }
            }
        });
        return resultsWrapper[0];
    }
}
