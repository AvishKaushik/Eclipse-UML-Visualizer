package views;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for adding/viewing notes (UI)
 */
// Please review carefully.
public class NoteDialog extends TitleAreaDialog {

    private Text contentText;
    private Text authorText;
    private Text timestampText;
    private String content;
    private String author;
    private String timestamp;
    private boolean viewOnly;
    
    public NoteDialog(Shell parentShell) {
        this(parentShell, false);
    }
    
    public NoteDialog(Shell parentShell, boolean viewOnly) {
        super(parentShell);
        this.viewOnly = viewOnly;
    }
    
    @Override
    public void create() {
        super.create();
        if (viewOnly) {
            setTitle("View Note");
            setMessage("Note Details", IMessageProvider.INFORMATION);
        } else {
            setTitle("Create Note");
            setMessage("Add a note to this diagram element", IMessageProvider.INFORMATION);
        }
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        layout.verticalSpacing = 8;
        layout.horizontalSpacing = 10;
        container.setLayout(layout);

        // Show timestamp if viewing existing note
        if (viewOnly && timestamp != null) {
            createTimestampField(container);
        }

        createAuthorField(container);
        createContentField(container);

        return area;
    }
    
    private void createTimestampField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("Created:");
        label.setFont(org.eclipse.jface.resource.JFaceResources.getFontRegistry().getBold(org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT));

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;

        timestampText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        timestampText.setLayoutData(data);
        timestampText.setText(timestamp != null ? timestamp : "");
        timestampText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    }

    private void createAuthorField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("Author:");
        label.setFont(org.eclipse.jface.resource.JFaceResources.getFontRegistry().getBold(org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT));

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;

        authorText = new Text(container, SWT.BORDER);
        authorText.setLayoutData(data);

        if (author != null) {
            authorText.setText(author);
        } else {
            authorText.setText(System.getProperty("user.name"));
        }

        if (viewOnly) {
            authorText.setEnabled(false);
            authorText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }
    }
    
    private void createContentField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("Note:");
        label.setFont(org.eclipse.jface.resource.JFaceResources.getFontRegistry().getBold(org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT));
        GridData labelData = new GridData();
        labelData.verticalAlignment = SWT.TOP;
        label.setLayoutData(labelData);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.heightHint = 120;
        data.widthHint = 400;

        contentText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        contentText.setLayoutData(data);

        if (content != null) {
            contentText.setText(content);
        }

        if (viewOnly) {
            contentText.setEnabled(false);
            contentText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }
    
    @Override
    protected void okPressed() {
        content = contentText.getText();
        author = authorText.getText();
        super.okPressed();
    }
    
    public String getContent() {
        return content;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}