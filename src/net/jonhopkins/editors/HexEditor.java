package net.jonhopkins.editors;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class HexEditor extends JFrame {
    
    private static final long serialVersionUID = -6883684965713305215L;
    private final int scrollSpeed = 16;
    private JPanel mainPanel;
    private JPanel northPanel;
    private JPanel southPanel;
    private JTextArea txtBytes;
    private JTextArea txtAddr;
    private JTextArea txtHex;
    private JTextArea txtRepr;
    private JTextField txtFilename;
    private JButton btnOpenFile;
    private JButton btnPrevious;
    private JButton btnNext;
    private JTextField txtSkip;
    private JTextField txtSize;
    private JButton btnSaveBytes;
    private JButton btnSaveHex;
    
    private byte[] section;
    
    private RandomAccessFile file = null;
    
    public static void main(String[] args) {
        new HexEditor().start();
    }
    
    public void start() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initComponents();
                setVisible(true);
            }
        });
    }
    
    public void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        setLayout(new BorderLayout());
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 2, -1, -1));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        txtBytes = new JTextArea();
        txtBytes.setFont(new Font("monospaced", Font.PLAIN, 12));
        txtBytes.setMargin(new Insets(5, 5, 5, 5));
        txtBytes.setLineWrap(true);
        
        JScrollPane scrollPaneBytes = new JScrollPane(txtBytes);
        scrollPaneBytes.getVerticalScrollBar().setUnitIncrement(scrollSpeed);
        scrollPaneBytes.getHorizontalScrollBar().setUnitIncrement(scrollSpeed);
        mainPanel.add(scrollPaneBytes);
        
        JPanel hexPanel = new JPanel();
        hexPanel.setLayout(new BorderLayout());
        txtAddr = new JTextArea();
        txtAddr.setFont(new Font("monospaced", Font.PLAIN, 12));
        txtAddr.setMargin(new Insets(5, 5, 5, 5));
        txtAddr.setEditable( false );
        txtAddr.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int caretPos = txtAddr.getCaretPosition();
                int line = 0;
                
                int lineStart = -1;
                int lineEnd = -1;
                String lines = txtAddr.getText();
                while (lineEnd < caretPos) {
                    lineStart = lineEnd + 1;
                    lineEnd = lines.indexOf('\n', lineStart);
                    line++;
                }
                txtAddr.select(lineStart, lineEnd);
                
                lineStart = -1;
                lineEnd = -1;
                lines = txtHex.getText();
                for (int i = 0; i < line; i++) {
                    lineStart = lineEnd + 1;
                    lineEnd = lines.indexOf('\n', lineStart);
                }
                txtHex.grabFocus();
                txtHex.select(lineStart, lineEnd);
                
                lineStart = -1;
                lineEnd = -1;
                lines = txtRepr.getText();
                for (int i = 0; i < line; i++) {
                    lineStart = lineEnd + 1;
                    lineEnd = lines.indexOf('\n', lineStart);
                }
                txtRepr.grabFocus();
                txtRepr.select(lineStart, lineEnd);
                
                txtAddr.grabFocus();
            }
        });
        hexPanel.add(txtAddr, BorderLayout.WEST);
        
        txtHex = new JTextArea();
        txtHex.setFont(new Font("monospaced", Font.PLAIN, 12));
        txtHex.setMargin(new Insets(5, 5, 5, 5));
        hexPanel.add(txtHex, BorderLayout.CENTER);
        
        txtRepr = new JTextArea();
        txtRepr.setFont(new Font("monospaced", Font.PLAIN, 12));
        txtRepr.setMargin(new Insets(5, 5, 5, 5));
        hexPanel.add(txtRepr, BorderLayout.EAST);
        
        JScrollPane scrollPaneHex = new JScrollPane(hexPanel);
        scrollPaneHex.getVerticalScrollBar().setUnitIncrement(scrollSpeed);
        scrollPaneHex.getHorizontalScrollBar().setUnitIncrement(scrollSpeed);
        mainPanel.add(scrollPaneHex);
        
        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 6));
        
        btnSaveBytes = new JButton("Save Bytes");
        btnSaveBytes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                writeBytes();
            }
        });
        northPanel.add(btnSaveBytes);
        
        btnPrevious = new JButton("<");
        btnPrevious.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveBack();
                readSection();
            }
        });
        btnPrevious.setEnabled(false);
        northPanel.add(btnPrevious);
        
        txtSkip = new JTextField();
        txtSkip.setText("1");
        northPanel.add(txtSkip);
        
        txtSize = new JTextField();
        txtSize.setText("512");
        txtSize.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (txtSize.getText().isEmpty()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    readSection();
                }
            }
        });
        northPanel.add(txtSize);
        
        btnNext = new JButton(">");
        btnNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveForward();
                readSection();
            }
        });
        btnNext.setEnabled(false);
        northPanel.add(btnNext);
        
        btnSaveHex = new JButton("Save Hex");
        btnSaveHex.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                writeHex();
            }
        });
        northPanel.add(btnSaveHex);
        
        southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(1, 2));
        
        txtFilename = new JTextField();
        txtFilename.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (txtFilename.getText().isEmpty()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openFile();
                }
            }
        });
        southPanel.add(txtFilename);
        
        btnOpenFile = new JButton("Open");
        btnOpenFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (txtFilename.getText().isEmpty()) {
                    return;
                }
                openFile();
            }
        });
        southPanel.add(btnOpenFile);
        
        setTitle("HexEditor");
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        add(northPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }
    
    private void openFile() {
        try {
            if (file != null) {
                file.close();
            }
            file = new RandomAccessFile(new File(txtFilename.getText()), "r");
            readSection();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void moveBack() {
        try {
            long curPos = file.getFilePointer();
            int skip = Integer.parseInt(txtSkip.getText());
            int size = Integer.parseInt(txtSize.getText());
            int total = size * skip;
            if (curPos < total) {
                file.seek(0);
                btnPrevious.setEnabled(false);
            } else {
                file.seek(curPos - total);
            }
            if (!btnNext.isEnabled()) {
                btnNext.setEnabled(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void moveForward() {
        try {
            long curPos = file.getFilePointer();
            int skip = Integer.parseInt(txtSkip.getText());
            int size = Integer.parseInt(txtSize.getText());
            int total = size * skip;
            if (curPos + total > file.length()) {
                file.seek(file.length());
                btnNext.setEnabled(false);
            } else {
                file.seek(curPos + total);
            }
            if (!btnPrevious.isEnabled()) {
                btnPrevious.setEnabled(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void readSection() {
        try {
            int size = Integer.parseInt(txtSize.getText());
            byte[] bytes;
            long len = file.length();
            long curPos = file.getFilePointer();
            if (curPos >= len) {
                bytes = "end of file".getBytes();
            } else {
                if (len - curPos < size) {
                    size = (int)(len - curPos);
                }
                bytes = new byte[size];
                file.readFully(bytes);
                file.seek(curPos); // reset to start of section
                
                btnPrevious.setEnabled(curPos > 0);
                
                btnNext.setEnabled(curPos + size < len);
                
                section = bytes;
            }
            
            putBytes(bytes);
            putHex(bytes, curPos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void putBytes(byte[] bytes) {
        txtBytes.setText(new String(bytes));
    }
    
    private void putHex(byte[] bytes, long startAddress) {
        String[] hexOutput = dumpHexString(bytes, startAddress);
        txtAddr.setText(hexOutput[0]);
        txtHex.setText(hexOutput[1]);
        txtRepr.setText(hexOutput[2]);
    }
    
    private void writeBytes() {
        try {
            byte data[] = section;
            long curPos = file.getFilePointer();
            String filename = txtFilename.getText();
            filename += "_bytes_" + curPos + "_to_" + (curPos + data.length);
            FileOutputStream out = new FileOutputStream(filename);
            out.write(data);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void writeHex() {
        try {
            byte data[] = txtHex.getText().getBytes();
            long curPos = file.getFilePointer();
            String filename = txtFilename.getText();
            filename += "_hex_" + curPos + "_to_" + (curPos + section.length);
            FileOutputStream out = new FileOutputStream(filename);
            out.write(data);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    
    private String[] dumpHexString(byte[] array, long startAddress) {
        return dumpHexString(array, 0, array.length, startAddress);
    }
    
    private String[] dumpHexString(byte[] array, int offset, int length, long startAddress) {
        StringBuilder addr = new StringBuilder();
        StringBuilder hex = new StringBuilder();
        StringBuilder repr = new StringBuilder();
        
        byte[] line = new byte[16];
        int lineIndex = 0;
        
        addr.append("0x");
        addr.append(toHexString(offset + (int)startAddress));
        
        for (int i = offset; i < offset + length; i++) {
            if (lineIndex == 16) {
                for (int j = 0; j < 16; j++) {
                    if (line[j] > ' ' && line[j] < '~') {
                        repr.append(new String(line, j, 1));
                    } else {
                        repr.append(".");
                    }
                }
                repr.append('\n');
                
                addr.append("\n0x");
                addr.append(toHexString(i + (int)startAddress));
                lineIndex = 0;
                hex.setLength(hex.length() - 1);
                hex.append('\n');
            }
            
            byte b = array[i];
            hex.append(HEX_DIGITS[(b >>> 4) & 0x0F]);
            hex.append(HEX_DIGITS[b & 0x0F]);
            hex.append(' ');
             
            line[lineIndex++] = b;
        }
        
        for (int i = 0; i < lineIndex; i++) {
            if (line[i] > ' ' && line[i] < '~') {
                repr.append(new String(line, i, 1));
            } else {
                repr.append(".");
            }
        }
        
        return new String[] { addr.toString(), hex.toString(), repr.toString() };
    }
    
    private String toHexString(byte b) {
        return toHexString(toByteArray(b));
    }
    
    private String toHexString(byte[] array) {
        return toHexString(array, 0, array.length);
    }
    
    private String toHexString(byte[] array, int offset, int length) {
        char[] buf = new char[length * 2];
        
        int bufIndex = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = array[i];
            buf[bufIndex++] = HEX_DIGITS[(b >>> 4) & 0x0F];
            buf[bufIndex++] = HEX_DIGITS[b & 0x0F];
        }
        
        return new String(buf);        
    }
    
    private String toHexString(int i) {
        return toHexString(toByteArray(i));
    }
    
    private byte[] toByteArray(byte b) {
        byte[] array = new byte[1];
        array[0] = b;
        return array;
    }
    
    private byte[] toByteArray(int i) {
        byte[] array = new byte[4];
        
        array[3] = (byte)(i & 0xFF);
        array[2] = (byte)((i >> 8) & 0xFF);
        array[1] = (byte)((i >> 16) & 0xFF);
        array[0] = (byte)((i >> 24) & 0xFF);
        
        return array;
    }
    
    private int toByte(char c) {
        if (c >= '0' && c <= '9') return (c - '0');
        if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
        if (c >= 'a' && c <= 'f') return (c - 'a' + 10);
        
        throw new RuntimeException ("Invalid hex char '" + c + "'");
    }
    
    private byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] buffer = new byte[length / 2];
        
        for (int i = 0; i < length; i += 2) {
            buffer[i / 2] = (byte)((toByte(hexString.charAt(i)) << 4) | toByte(hexString.charAt(i+1)));
        }
        
        return buffer;
    }
}
