package messenger.gui;

import messenger.MCHerald;
import messenger.util.Constants;
import messenger.util.GUI;
import messenger.util.Language;
import messenger.util.ServerInfo;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;

public class AddServer extends JDialog implements GUI {

    private JTextField nameField, hostField, freqField;

    public AddServer(MCHerald herald, ServerTable owner){
        super(owner);

        try {
            this.setIconImage(Constants.createImage(Language.R.ICON_ADD, Language.ICON_ADD_DESCRIPTION));
        } catch (FileNotFoundException ignore) {}

        this.setTitle(Language.ADD_SERVER.TITLE);
        this.setModalityType(ModalityType.DOCUMENT_MODAL);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        this.nameField = new JTextField(30);
        this.hostField = new JTextField(30);
        this.freqField = new JTextField(30);
        this.nameField.setFont(Constants.APP_FONT);
        this.hostField.setFont(Constants.APP_FONT);
        this.freqField.setFont(Constants.APP_FONT);

        JLabel nameLabel = new JLabel(Language.ADD_SERVER.NAME);
        JLabel hostLabel = new JLabel(Language.ADD_SERVER.HOST);
        JLabel freqLabel = new JLabel(Language.ADD_SERVER.FREQUENCY);

        JPanel form = new JPanel(new GridBagLayout());
        addComp(form, nameLabel, 0, 0, GridBagConstraints.EAST);
        addComp(form, nameField, 1, 0, GridBagConstraints.WEST);
        addComp(form, hostLabel, 0, 1, GridBagConstraints.EAST);
        addComp(form, hostField, 1, 1, GridBagConstraints.WEST);
        addComp(form, freqLabel, 0, 2, GridBagConstraints.EAST);
        addComp(form, freqField, 1, 2, GridBagConstraints.WEST);

        JPanel buttons = new JPanel();
        JButton addButton = new JButton(Language.ADD_SERVER.SUBMIT);
        buttons.add(addButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        panel.add(form, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.SOUTH);

        this.setContentPane(panel);
        this.setResizable(false);
        this.pack();
        this.setLocationRelativeTo(owner);

        addButton.addActionListener(e -> {
            this.close();
            // TODO: validate input
            int frequency = (Integer.parseInt(freqField.getText()) > 0) ? Integer.parseInt(freqField.getText()) : Constants.DEFAULT_FREQUENCY;
            ServerInfo info = new ServerInfo(herald, hostField.getText().trim(), nameField.getText().trim(), true, frequency);
            if(info.equals(Constants.ERROR)) info = null;
            herald.addServer(info);
        });
    }

    /* Private Methods */

    /**
     * @author Derek Banas
     * @see "http://www.newthinktank.com/2012/03/java-video-tutorial-30/"
     * **/
    private void addComp(JPanel thePanel, JComponent comp, int xPos, int yPos, int place){
        GridBagConstraints gridConstraints = new GridBagConstraints();
        gridConstraints.gridx = xPos;
        gridConstraints.gridy = yPos;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.weightx = 100;
        gridConstraints.weighty = 100;
        gridConstraints.insets = new Insets(5,5,5,5);
        gridConstraints.anchor = place;
        gridConstraints.fill = GridBagConstraints.NONE;
        thePanel.add(comp, gridConstraints);
    }

    /* Contract Methods */

    @Override
    public void open(){
        hostField.setText("");
        nameField.setText("");
        freqField.setText("");
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }

    @Override
    public void close(){
        SwingUtilities.invokeLater(() -> this.setVisible(false));
    }

    @Override
    public void shutdown(){
        SwingUtilities.invokeLater(() -> {
            this.setVisible(false);
            this.dispose();
        });
    }
}
