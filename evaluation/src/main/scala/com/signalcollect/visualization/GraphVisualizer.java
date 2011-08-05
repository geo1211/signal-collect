package com.signalcollect.visualization;

import javax.swing.DefaultListModel;
import com.signalcollect.interfaces.Vertex;
import com.signalcollect.configuration.DefaultExecutionConfiguration;
import com.signalcollect.configuration.SynchronousExecutionMode;
import com.signalcollect.visualization.ComputeGraphInspector;

/**
 *
 * @author Philip Stutz
 */
public class GraphVisualizer extends javax.swing.JFrame {
	
	private ComputeGraphInspector cgi;
    
    /** Creates new form GraphVisualizer */
    public GraphVisualizer(ComputeGraphInspector cgi) {
        this.cgi = cgi;
        initComponents();
        jungPanel.setComputeGraphInspector(cgi);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        searchVertexField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        vertexList = new javax.swing.JList();
        searchVertexButton = new javax.swing.JButton();
        depthSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        computationStepButton = new javax.swing.JButton();
        jungPanel = new com.signalcollect.visualization.JungPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        vertexList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                vertexListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(vertexList);

        searchVertexButton.setText("Search Vertex");
        searchVertexButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchVertexButtonActionPerformed(evt);
            }
        });

        depthSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 0, 20, 1));
        depthSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                depthSpinnerStateChanged(evt);
            }
        });

        jLabel1.setText("Paint Depth");

        computationStepButton.setText("Computation Step");
        computationStepButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                computationStepButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jungPanelLayout = new org.jdesktop.layout.GroupLayout(jungPanel);
        jungPanel.setLayout(jungPanelLayout);
        jungPanelLayout.setHorizontalGroup(
            jungPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 457, Short.MAX_VALUE)
        );
        jungPanelLayout.setVerticalGroup(
            jungPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 408, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(searchVertexField)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(searchVertexButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 244, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(18, 18, 18)
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(depthSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 168, Short.MAX_VALUE)
                        .add(computationStepButton))
                    .add(layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jungPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(searchVertexField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(searchVertexButton)
                        .add(jLabel1))
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                        .add(computationStepButton)
                        .add(depthSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jungPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchVertexButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchVertexButtonActionPerformed
        Iterable<Vertex> vertices = (Iterable<Vertex>) cgi.searchVertex(searchVertexField.getText());
        DefaultListModel lm = new DefaultListModel();
        for (Vertex v : vertices) {
            lm.addElement(v);
        }
        vertexList.setModel(lm);
    }//GEN-LAST:event_searchVertexButtonActionPerformed

    private void vertexListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_vertexListValueChanged
        redraw();
    }//GEN-LAST:event_vertexListValueChanged

    private void depthSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_depthSpinnerStateChanged
        redraw();
    }//GEN-LAST:event_depthSpinnerStateChanged

    private void computationStepButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_computationStepButtonActionPerformed
        cgi.executeComputationStep();
        redraw();
    }//GEN-LAST:event_computationStepButtonActionPerformed

    public void redraw() {
        Vertex selectedVertex = (Vertex) vertexList.getSelectedValue();
        if (selectedVertex != null) {
         jungPanel.paintVertex(selectedVertex, (Integer) depthSpinner.getValue());
        }
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new GraphVisualizer(new ComputeGraphInspector(null)).setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton computationStepButton;
    private javax.swing.JSpinner depthSpinner;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private com.signalcollect.visualization.JungPanel jungPanel;
    private javax.swing.JButton searchVertexButton;
    private javax.swing.JTextField searchVertexField;
    private javax.swing.JList vertexList;
    // End of variables declaration//GEN-END:variables
}