import com.intellij.openapi.ui.DialogWrapper;
import com.siyeh.ig.ui.TextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by ville on 4/14/16.
 */
public class NeovimDialog extends DialogWrapper {

    private JTextField mComponent;

    public NeovimDialog(boolean canBeParent) {
        super(canBeParent);

        this.setTitle("Connect to Neovim");
        mComponent = new JTextField();
        mComponent.setToolTipText("Neovim TCP address");

        this.init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mComponent;
    }

    public String getAddr() {
        return mComponent.getText();
    }
}
