/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package main;

import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author bagusade_s
 */
public class form_prosesData extends javax.swing.JPanel {

    private Connection conn;
    private static final Logger logger = Logger.getLogger(form_prosesData.class.getName());

    // Data kriteria (urutan index 0-9 sejajar dengan kolom c1-c10)
    private String[] namaKriteria = new String[10];
    private String[] jenisKriteria = new String[10];
    private double[] bobot = new double[10];

    // Data penilaian
    private List<String> kodeKaryawan = new ArrayList<>();
    private List<String> namaKaryawan = new ArrayList<>();
    private double[][] matrix;       // nilai asli
    private double[][] normalisasi;  // hasil normalisasi
    private double[] nilaiPreferensi; // hasil Vi

    private boolean sudahNormalisasi = false;

    public form_prosesData() {
        initComponents();
        conn = Koneksi.getConnection();
        btn_rank.setEnabled(false); // rank baru bisa diklik setelah normalisasi
    }
        
    // ================= LOAD KRITERIA =================
    private boolean loadKriteria() {
        String sql = "SELECT id, nama_kriteria, jenis_kriteria, bobot FROM kriteria ORDER BY id ASC";
        try (PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            int i = 0, total = 0;
            while (rs.next()) {
                total++;
                if (i < 10) {
                    namaKriteria[i] = rs.getString("nama_kriteria");
                    jenisKriteria[i] = rs.getString("jenis_kriteria");
                    bobot[i] = rs.getDouble("bobot");
                    i++;
                }
            }
            if (total != 10) {
                JOptionPane.showMessageDialog(this,
                    "Kriteria harus tepat 10 (ditemukan " + total + ").\n"
                    + "Data yang diambil = 10 baris pertama ORDER BY id, sisanya diabaikan\n"
                    + "senyap sehingga ranking bisa salah. Bersihkan tabel kriteria.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading kriteria", e);
            JOptionPane.showMessageDialog(this, "Gagal memuat data kriteria.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ================= LOAD PENILAIAN =================
    private boolean loadPenilaian() {
        kodeKaryawan.clear();
        namaKaryawan.clear();
        List<double[]> rows = new ArrayList<>();

        String sql = "SELECT p.kode_karyawan, k.nama, "
                   + "p.c1, p.c2, p.c3, p.c4, p.c5, p.c6, p.c7, p.c8, p.c9, p.c10 "
                   + "FROM penilaian p JOIN karyawan k ON p.kode_karyawan = k.kode_karyawan";

        try (PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                kodeKaryawan.add(rs.getString("kode_karyawan"));
                namaKaryawan.add(rs.getString("nama"));
                double[] row = new double[10];
                for (int j = 0; j < 10; j++) {
                    row[j] = rs.getDouble("c" + (j + 1));
                }
                rows.add(row);
            }
            if (rows.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Belum ada data penilaian!", "Info", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            matrix = rows.toArray(new double[0][]);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading penilaian", e);
            JOptionPane.showMessageDialog(this, "Gagal memuat data penilaian.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ================= HITUNG NORMALISASI =================
    private void hitungNormalisasi() {
        int totalKaryawan = matrix.length;
        normalisasi = new double[totalKaryawan][10];

        for (int j = 0; j < 10; j++) {
            double max = matrix[0][j];
            double min = matrix[0][j];
            for (int i = 1; i < totalKaryawan; i++) {
                max = Math.max(max, matrix[i][j]);
                min = Math.min(min, matrix[i][j]);
            }
            for (int i = 0; i < totalKaryawan; i++) {
                if ("Cost".equalsIgnoreCase(jenisKriteria[j])) {
                    normalisasi[i][j] = (matrix[i][j] == 0) ? 0 : min / matrix[i][j];
                } else { // Benefit
                    normalisasi[i][j] = (max == 0) ? 0 : matrix[i][j] / max;
                }
            }
        }
    }

    // ================= HITUNG RANKING =================
    private void hitungRanking() {
        int totalKaryawan = matrix.length;
        nilaiPreferensi = new double[totalKaryawan];
        for (int i = 0; i < totalKaryawan; i++) {
            double vi = 0;
            for (int j = 0; j < 10; j++) {
                vi += normalisasi[i][j] * bobot[j];
            }
            nilaiPreferensi[i] = vi;
        }
    }

    // ================= TAMPILKAN TABEL NORMALISASI =================
    private void tampilkanNormalisasi() {
        DefaultTableModel model = (DefaultTableModel) tabel_normalisasi.getModel();
        model.setRowCount(0);
        model.setColumnCount(0);

        model.addColumn("No.");
        model.addColumn("Kode");
        model.addColumn("Nama");
        for (int j = 0; j < 10; j++) {
            model.addColumn("C" + (j + 1));
        }

        for (int i = 0; i < matrix.length; i++) {
            Object[] row = new Object[13];
            row[0] = i + 1;
            row[1] = kodeKaryawan.get(i);
            row[2] = namaKaryawan.get(i);
            for (int j = 0; j < 10; j++) {
                row[3 + j] = String.format("%.4f", normalisasi[i][j]);
            }
            model.addRow(row);
        }
    }

    // ================= TAMPILKAN TABEL RANKING =================
    private void tampilkanRanking(Integer[] index) {
        DefaultTableModel model = (DefaultTableModel) tabel_ranking.getModel();
        model.setRowCount(0);

        for (int rank = 0; rank < index.length; rank++) {
            int i = index[rank];
            Object[] row = {
                rank + 1,
                kodeKaryawan.get(i),
                namaKaryawan.get(i),
                String.format("%.4f", nilaiPreferensi[i])
            };
            model.addRow(row);
        }
    }
    
    // ================= SIMPAN HASIL RANKING KE DB =================
    private boolean simpanHasilRanking(Integer[] index) {
        String deleteSql = "DELETE FROM hasil_ranking";
        String insertSql = "INSERT INTO hasil_ranking (kode_karyawan, nilai_preferensi, rank_position) "
                          + "VALUES (?, ?, ?)";

        try {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                for (int rank = 0; rank < index.length; rank++) {
                    int i = index[rank];
                    ins.setString(1, kodeKaryawan.get(i));
                    ins.setDouble(2, nilaiPreferensi[i]);
                    ins.setInt(3, rank + 1);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            conn.commit();
            return true;
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            logger.log(Level.SEVERE, "Error saving hasil ranking", e);
            JOptionPane.showMessageDialog(this, "Gagal menyimpan hasil ranking ke database.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabel_normalisasi = new javax.swing.JTable();
        btn_normalisasi = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tabel_ranking = new javax.swing.JTable();
        btn_rank = new javax.swing.JButton();
        btn_reset = new javax.swing.JButton();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new java.awt.CardLayout());

        mainPanel.setBackground(new java.awt.Color(255, 255, 255));
        mainPanel.setLayout(new java.awt.CardLayout());

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel6.setBackground(new java.awt.Color(0, 0, 0));
        jLabel6.setFont(new java.awt.Font("Plus Jakarta Sans ExtraBold", 0, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(41, 54, 129));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("PROSES DATA");

        jSeparator2.setForeground(java.awt.Color.darkGray);

        tabel_normalisasi.setBackground(new java.awt.Color(208, 231, 230));
        tabel_normalisasi.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(149, 204, 221)));
        tabel_normalisasi.setForeground(new java.awt.Color(41, 54, 129));
        tabel_normalisasi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "No.", "Kode", "Nama", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10"
            }
        ));
        tabel_normalisasi.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        tabel_normalisasi.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabel_normalisasiMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(tabel_normalisasi);

        btn_normalisasi.setBackground(new java.awt.Color(41, 54, 129));
        btn_normalisasi.setFont(new java.awt.Font("Plus Jakarta Sans ExtraBold", 0, 13)); // NOI18N
        btn_normalisasi.setForeground(new java.awt.Color(255, 255, 255));
        btn_normalisasi.setText("NORMALISASI");
        btn_normalisasi.addActionListener(this::btn_normalisasiActionPerformed);

        tabel_ranking.setBackground(new java.awt.Color(208, 231, 230));
        tabel_ranking.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(149, 204, 221)));
        tabel_ranking.setForeground(new java.awt.Color(41, 54, 129));
        tabel_ranking.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Rank", "Kode", "Nama Karyawan", "Nilai Preferensi"
            }
        ));
        tabel_ranking.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        tabel_ranking.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabel_rankingMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(tabel_ranking);

        btn_rank.setBackground(new java.awt.Color(41, 54, 129));
        btn_rank.setFont(new java.awt.Font("Plus Jakarta Sans ExtraBold", 0, 13)); // NOI18N
        btn_rank.setForeground(new java.awt.Color(255, 255, 255));
        btn_rank.setText("RANK");
        btn_rank.addActionListener(this::btn_rankActionPerformed);

        btn_reset.setBackground(new java.awt.Color(41, 54, 129));
        btn_reset.setFont(new java.awt.Font("Plus Jakarta Sans ExtraBold", 0, 13)); // NOI18N
        btn_reset.setForeground(new java.awt.Color(255, 255, 255));
        btn_reset.setText("RESET");
        btn_reset.addActionListener(this::btn_resetActionPerformed);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
                    .addComponent(jSeparator2)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_normalisasi)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(btn_rank)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_reset)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 351, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_normalisasi, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 351, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_rank, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_reset, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(121, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel3);

        mainPanel.add(jScrollPane1, "card2");

        jPanel1.add(mainPanel, "card2");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 912, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tabel_normalisasiMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabel_normalisasiMouseClicked
       
    }//GEN-LAST:event_tabel_normalisasiMouseClicked

    private void btn_normalisasiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_normalisasiActionPerformed
        if (!loadKriteria()) return;
        if (!loadPenilaian()) return;

        hitungNormalisasi();
        tampilkanNormalisasi();

        sudahNormalisasi = true;
        btn_rank.setEnabled(true);

        JOptionPane.showMessageDialog(this, "Normalisasi berhasil dihitung!");
    }//GEN-LAST:event_btn_normalisasiActionPerformed

    private void tabel_rankingMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabel_rankingMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_tabel_rankingMouseClicked

    private void btn_rankActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_rankActionPerformed
        if (!sudahNormalisasi) {
            JOptionPane.showMessageDialog(this, "Lakukan normalisasi terlebih dahulu!", "Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        hitungRanking();

        Integer[] index = new Integer[nilaiPreferensi.length];
        for (int i = 0; i < index.length; i++) index[i] = i;
        Arrays.sort(index, (a, b) -> Double.compare(nilaiPreferensi[b], nilaiPreferensi[a]));

        tampilkanRanking(index);
        if (simpanHasilRanking(index)) {
            JOptionPane.showMessageDialog(this, "Perangkingan selesai dan tersimpan!");
        }
    }//GEN-LAST:event_btn_rankActionPerformed

    private void btn_resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_resetActionPerformed
        DefaultTableModel modelNormalisasi = (DefaultTableModel) tabel_normalisasi.getModel();
        modelNormalisasi.setRowCount(0);
        modelNormalisasi.setColumnCount(0);
        modelNormalisasi.addColumn("No.");
        modelNormalisasi.addColumn("Kode");
        modelNormalisasi.addColumn("Nama");
        for (int j = 1; j <= 10; j++) {
            modelNormalisasi.addColumn("C" + j);
        }

        DefaultTableModel modelRanking = (DefaultTableModel) tabel_ranking.getModel();
        modelRanking.setRowCount(0);

        matrix = null;
        normalisasi = null;
        nilaiPreferensi = null;
        kodeKaryawan.clear();
        namaKaryawan.clear();
        sudahNormalisasi = false;
        btn_rank.setEnabled(false);
    }//GEN-LAST:event_btn_resetActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_normalisasi;
    private javax.swing.JButton btn_rank;
    private javax.swing.JButton btn_reset;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTable tabel_normalisasi;
    private javax.swing.JTable tabel_ranking;
    // End of variables declaration//GEN-END:variables
}
