package codeinspect;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.util.ui.UIUtil;
import complete.EmbeditorUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ville on 4/19/16.
 */
public class Inspect {
    private final static Logger LOG = Logger.getInstance(Inspect.class);

    /**
     * Runs code analyzis and returns all problems found under cursor (row and col).
     *
     * @param path
     * @param fileContent
     * @param row
     * @param col
     * @return
     */
    public static List<HighlightInfo.IntentionActionDescriptor> getFixes(
            final String path, @Nullable final String fileContent, final int row, final int col) {
        List<HighlightInfo.IntentionActionDescriptor> fixes = new ArrayList<>();
        Pair<Document, List<HighlightInfo>> problems = getProblems(path, fileContent);
        Document doc = problems.getFirst();
        for (HighlightInfo h : problems.getSecond()) {
            if (h.quickFixActionRanges == null) continue;
            for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> p : h.quickFixActionRanges) {
                int offset = EmbeditorUtil.lineAndColumnToOffset(doc, row, col);
                if (p.getSecond().contains(offset)) {
                    fixes.add(p.getFirst());
                }
            }
        }
        return fixes;
    }

    /**
     * Runs code anlyzis on document found in path and returns all problems found with the document.
     * @param path
     * @param fileContent
     * @return
     */
    private static Pair<Document, List<HighlightInfo>> getProblems(final String path, @Nullable final String fileContent) {
        final Ref<PsiFile> psiFileRef = new Ref<>();
        final Ref<Editor> editorRef = new Ref<>();
        final Ref<Document> docRef = new Ref<>();
        final Ref<Project> projectRef = new Ref<>();

        UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
            PsiFile targetPsiFile = EmbeditorUtil.findTargetFile(path);
            VirtualFile targetVirtualFile =  targetPsiFile.getVirtualFile();
            Project project = targetPsiFile.getProject();

            PsiFile fileCopy = fileContent != null
                    ? EmbeditorUtil.createDummyPsiFile(project, fileContent, targetPsiFile)
                    : EmbeditorUtil.createDummyPsiFile(project, targetPsiFile.getText(), targetPsiFile);

            final Document document = fileCopy.getViewProvider().getDocument();

            editorRef.set(EditorFactory.getInstance().createEditor(document, project, targetVirtualFile, false));
            psiFileRef.set(targetPsiFile);
            docRef.set(document);
            projectRef.set(project);
        });
        Disposable context = Disposer.newDisposable();

        Ref<List<HighlightInfo>> highlightInfoList = new Ref<>();

        ApplicationManager.getApplication().runReadAction(() -> {
            final DaemonProgressIndicator progress = new DaemonProgressIndicator();
            Disposer.register(context, progress);

            ProgressManager.getInstance().runProcess(() -> {

                final DaemonCodeAnalyzerEx analyzer =
                        DaemonCodeAnalyzerEx.getInstanceEx(projectRef.get());
                //analyzer.restart(psiFileRef.get());

                // analyze!
                highlightInfoList.set(analyzer.runMainPasses(
                        psiFileRef.get(), docRef.get(), progress));
            }, progress);
        });
        return Pair.create(docRef.get(), highlightInfoList.get());
    }

    /**
     * Invokes action in intentionActionDescriptor on file found in path and writes the file to disk.
     *
     * @param path
     * @param fileContent
     * @param intentionActionDescriptor
     * @return
     */
    public static String doFix(String path, @Nullable String fileContent, HighlightInfo.IntentionActionDescriptor intentionActionDescriptor) {
        UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
            PsiFile psiFile = EmbeditorUtil.findTargetFile(path);
            Project project = psiFile.getProject();

            PsiFile fileCopy = fileContent != null
                    ? EmbeditorUtil.createDummyPsiFile(project, fileContent, psiFile)
                    : EmbeditorUtil.createDummyPsiFile(project, psiFile.getText(), psiFile);

            VirtualFile targetVirtualFile =  psiFile.getVirtualFile();
            Document document = fileCopy.getViewProvider().getDocument();

            Editor editor = EditorFactory.getInstance().createEditor(document, project, targetVirtualFile, false);

            intentionActionDescriptor.getAction().invoke(project, editor, fileCopy);

            FileDocumentManager.getInstance().saveDocument(psiFile.getViewProvider().getDocument());
        });
        return null;
    }
}
