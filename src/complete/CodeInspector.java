package complete;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.daemon.impl.DefaultHighlightInfoProcessor;
import com.intellij.codeInsight.daemon.impl.LocalInspectionsPass;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.editor.Document;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiFile;

import java.util.Arrays;
import java.util.List;

public class CodeInspector {
    public static List<Problem> inspect(PsiFile file) {
        // TODO: See the internals InspectionApplication.run()
        InspectionManagerEx manager = (InspectionManagerEx)InspectionManager.getInstance(file.getProject());
        InspectionProfileManager profileManager = InspectionProfileManager.getInstance();
        InspectionProfile profile = (InspectionProfile)profileManager.getProfile(manager.getCurrentProfile(), false);
        InspectionToolWrapper[] tools = profile.getInspectionTools(null);
        GlobalInspectionContextImpl context = manager.createNewGlobalContext(true);
        AnalysisScope scope = new AnalysisScope(file);
        Document document = file.getViewProvider().getDocument();
        LocalInspectionsPass pass = new LocalInspectionsPass(file, document, 0, file.getTextLength(), LocalInspectionsPass.EMPTY_PRIORITY_RANGE,
                true, new DefaultHighlightInfoProcessor());
        return Arrays.asList(
                new Problem(10, 4, "Undefined name 'foo'"),
                new Problem(12, 6, "Undefined name 'bar'")
        );
    }
}


