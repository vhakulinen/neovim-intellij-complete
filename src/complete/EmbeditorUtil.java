package complete;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * User: zolotov
 * Date: 5/24/13
 */
public final class EmbeditorUtil {
  private EmbeditorUtil() {
  }

  public static int getCompletionPrefixLength(@NotNull CompletionParameters completionParameters) {
    return completionParameters.getPosition().getTextLength() - CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.length();
  }

  public static int getOffsetFromLineStart(@NotNull CompletionParameters completionParameters, @NotNull Document document) {
    TextRange range = completionParameters.getPosition().getTextRange();
    if (range != null) {
      int offset = range.getStartOffset();
      int lineNumber = document.getLineNumber(offset);
      return offset - document.getLineStartOffset(lineNumber);
    }
    return 0;
  }

  @NotNull
  public static ResolveOutcome getSingleResolveOutcome(@NotNull final String path,
                                                       @NotNull final String fileContent,
                                                       final int line,
                                                       final int column) {
    Collection<ResolveOutcome> resolveOutcome = getResolveOutcomes(path, fileContent, line, column);
    return resolveOutcome.size() == 1
           ? ContainerUtil.getFirstItem(resolveOutcome, ResolveOutcome.NULL)
           : ResolveOutcome.NULL;
  }

  @NotNull
  public static List<ResolveOutcome> getResolveOutcomes(@NotNull final String path,
                                                        @NotNull final String fileContent,
                                                        final int line,
                                                        final int column) {
    final Ref<List<ResolveOutcome>> ref = Ref.create();
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        PsiElement[] elements = performResolve(path, fileContent, line, column);
        if (elements.length > 0) {
          List<ResolveOutcome> result = new LinkedList<ResolveOutcome>();
          for (PsiElement element : elements) {
            if (element != null) {
              PsiFile resolveToFile = element.getContainingFile();
              if (resolveToFile != null) {
                VirtualFile virtualFile = resolveToFile.getOriginalFile().getVirtualFile();
                String resolveToPath = virtualFile != null ? virtualFile.getPath() : path;
                Document doc = resolveToFile.getViewProvider().getDocument();

                assert doc != null;
                int offset = element.getTextOffset();
                int resolveToRow = doc.getLineNumber(offset);
                int lineStartOffset = doc.getLineStartOffset(resolveToRow);
                int resolveToColumn = offset - lineStartOffset;
                String resolveToText = SymbolPresentationUtil.getSymbolPresentableText(element);
                result.add(new ResolveOutcome(resolveToPath, resolveToRow, resolveToColumn, resolveToText));
              }
            }
          }
          ref.set(result);
        }
      }
    });
    return !ref.isNull() ? ref.get() : Collections.<ResolveOutcome>emptyList();
  }

  @NotNull
  private static PsiElement[] performResolve(@NotNull final String path,
                                             @Nullable final String fileContent,
                                             final int line,
                                             final int column) {
    final PsiFile targetPsiFile = findTargetFile(path);
    final VirtualFile targetVirtualFile = targetPsiFile != null ? targetPsiFile.getVirtualFile() : null;
    if (targetPsiFile != null && targetVirtualFile != null) {
      final Project project = targetPsiFile.getProject();
      final Document originalDocument = PsiDocumentManager.getInstance(project).getDocument(targetPsiFile);
      if (originalDocument != null) {

        PsiFile fileCopy = fileContent != null
                           ? createDummyFile(project, fileContent, targetPsiFile)
                           : createDummyFile(project, targetPsiFile.getText(), targetPsiFile);
        final Document document = fileCopy.getViewProvider().getDocument();
        if (document != null) {
          int offset = lineAndColumnToOffset(document, line, column);
          PsiReference reference = fileCopy.findReferenceAt(offset);
          if (reference instanceof PsiPolyVariantReference) {
            ResolveResult[] resolveResults = ((PsiPolyVariantReference)reference).multiResolve(true);
            PsiElement[] elements = new PsiElement[resolveResults.length];
            for (int i = 0; i < resolveResults.length; i++) {
              elements[i] = resolveResults[i].getElement();
            }
            return elements;
          }
          else if (reference != null) {
            return new PsiElement[]{reference.resolve()};
          }
        }
      }
    }
    return PsiElement.EMPTY_ARRAY;
  }

  public static void performCompletion(@NotNull final String path,
                                       @Nullable final String fileContent,
                                       final int line,
                                       final int column,
                                       @NotNull final CompletionCallback completionCallback) {
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        final PsiFile targetPsiFile = findTargetFile(path);
        final VirtualFile targetVirtualFile = targetPsiFile != null ? targetPsiFile.getVirtualFile() : null;
        if (targetPsiFile != null && targetVirtualFile != null) {
          final EditorFactory editorFactory = EditorFactory.getInstance();
          final Project project = targetPsiFile.getProject();
          final Document originalDocument = PsiDocumentManager.getInstance(project).getDocument(targetPsiFile);
          if (originalDocument != null) {

            PsiFile fileCopy = fileContent != null
                               ? createDummyFile(project, fileContent, targetPsiFile)
                               : createDummyFile(project, targetPsiFile.getText(), targetPsiFile);
            final Document document = fileCopy.getViewProvider().getDocument();
            if (document != null) {
              final Editor editor = editorFactory.createEditor(document, project, targetVirtualFile, false);
              int offset = lineAndColumnToOffset(document, line, column);
              editor.getCaretModel().moveToOffset(offset);
              CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                @Override
                public void run() {
                  final CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(CompletionType.BASIC) {

                    @Override
                    protected void completionFinished(@NotNull CompletionProgressIndicator indicator,
                                                      boolean hasModifiers) {
                      CompletionServiceImpl.setCompletionPhase(new CompletionPhase.ItemsCalculated(indicator));
                      completionCallback.completionFinished(indicator.getParameters(), indicator, document);
                    }
                  };

                  handler.invokeCompletion(project, editor);
                }
              }, null, null);
            }
          }
        }
      }
    });
  }

  public static PsiFile createDummyFile(Project project, String contents, PsiFile original) {
    final PsiFileFactory factory = PsiFileFactory.getInstance(project);
    final LightVirtualFile virtualFile = new LightVirtualFile(original.getName(), original.getFileType(), contents);

    final PsiFile psiFile = ((PsiFileFactoryImpl)factory).trySetupPsiForFile(virtualFile, original.getLanguage(), false, true);
    assert psiFile != null;
    return psiFile;
  }

  private static int lineAndColumnToOffset(Document document, int line, int column) {
    return document.getLineStartOffset(line) + column;
  }

  @Nullable
  public static PsiFile findTargetFile(@NotNull String path) {
    Pair<VirtualFile, Project> data = new File(path).isAbsolute() ? findByAbsolutePath(path) : findByRelativePath(path);
    return data != null ? PsiManager.getInstance(data.second).findFile(data.first) : null;
  }

  @Nullable
  public static Pair<VirtualFile, Project> findByAbsolutePath(@NotNull String path) {
    File file = new File(FileUtil.toSystemDependentName(path));
    if (file.exists()) {
      VirtualFile vFile = findVirtualFile(file);
      if (vFile != null) {
        Project project = ProjectLocator.getInstance().guessProjectForFile(vFile);
        if (project != null) {
          return Pair.create(vFile, project);
        }
      }
    }

    return null;
  }

  @Nullable
  public static Pair<VirtualFile, Project> findByRelativePath(@NotNull String path) {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    String localPath = FileUtil.toSystemDependentName(path);

    for (Project project : projects) {
      File file = new File(project.getBasePath(), localPath);
      if (file.exists()) {
        VirtualFile vFile = findVirtualFile(file);
        return vFile != null ? Pair.create(vFile, project) : null;
      }
    }

    for (Project project : projects) {
      for (VcsRoot vcsRoot : ProjectLevelVcsManager.getInstance(project).getAllVcsRoots()) {
        VirtualFile root = vcsRoot.getPath();
        if (root != null) {
          File file = new File(FileUtil.toSystemDependentName(root.getPath()), localPath);
          if (file.exists()) {
            VirtualFile vFile = findVirtualFile(file);
            return vFile != null ? Pair.create(vFile, project) : null;
          }
        }
      }
    }

    return null;
  }

  @Nullable
  public static VirtualFile findVirtualFile(@NotNull final File file) {
    return ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Nullable
      @Override
      public VirtualFile compute() {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      }
    });
  }

  public static interface CompletionCallback {
    void completionFinished(@NotNull CompletionParameters parameters,
                            @NotNull CompletionProgressIndicator indicator,
                            @NotNull Document document);
  }
}
