package view;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class PrimeiraTela extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PrimeiraTela frame = new PrimeiraTela();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public PrimeiraTela() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JButton btnAo = new JButton("Listar Usuarios Conectados");
		btnAo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				JOptionPane.showMessageDialog (null, "Listar Usuários");
				
			}
		});
		btnAo.setBounds(10, 22, 214, 23);
		contentPane.add(btnAo);
		
		JButton btnSair = new JButton("Sair");
		btnSair.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				PrimeiraTela.this.dispose();
				
			}
		});
		btnSair.setBounds(335, 214, 89, 23);
		contentPane.add(btnSair);
		
		JButton btnBuscarArquivo = new JButton("Buscar Arquivo");
		btnBuscarArquivo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				new TelaDeBusca().setVisible(true);
				dispose();
				
			}
		});
		btnBuscarArquivo.setBounds(10, 97, 214, 23);
		contentPane.add(btnBuscarArquivo);
	}
}
