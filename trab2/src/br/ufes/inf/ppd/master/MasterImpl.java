package br.ufes.inf.ppd.master;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jms.Message;

import com.sun.messaging.Queue;

import br.ufes.inf.ppd.Guess;
import br.ufes.inf.ppd.Master;
import br.ufes.inf.ppd.utils.DictionaryReader;
import br.ufes.inf.ppd.utils.Partition;

public class MasterImpl implements Master {

//	Diretorio base
	private Set<Partition> dictionaryPartitions;
//	Mapa que indica quais particoes um escravo esta responsavel em um ataque
	private Map<Integer, Attack> attacks;
//	Fila de sub-ataques a serem depositados pelo mestre
	private Queue subAttacksQueue;

	/**
	 * Construtor do mestre, aqui tambem inicializamos uma thread para verificar quais escravos
	 * do sistema ainda estao vivos. Essa thread eh disparada a cada 5 segundos.
	 * 
	 * @param dictionaryPath
	 * @param subAttacksQueue 
	 */
	public MasterImpl(String dictionaryPath, int numberOfPartitions, Queue subAttacksQueue) {
		DictionaryReader dictionaryReader = new DictionaryReader(dictionaryPath);
		
		this.dictionaryPartitions = dictionaryReader.toPartitions(numberOfPartitions);
		this.attacks = Collections.synchronizedMap(new HashMap<Integer, Attack>());
		this.subAttacksQueue = subAttacksQueue;
	}
	
	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
//		Incializa uma classe de ataque com os dados de entrada do cliente
		Attack attack = new Attack(cipherText, knownText);
		
//		O numero de ataque eh gerado automaticamente pela classe Attack, logo basta recupera-lo
		Integer attackNumber = attack.getAttackNumber();

		System.out.println("The attack [" + attackNumber + "] started");
		
//		Adicionamos esse ataque ao mapa de ataques que estao sendo gerenciados pelo mestre
		synchronized (attacks) {
			attacks.put(attackNumber, attack);	
			attack.setPartitions(dictionaryPartitions);
		}
		
		/*
		 * TODO coloca a particao e o numero do ataque na fila de subAttacks para
		 * que algum escravo a colete, processe toda a particao e retorne os chutes
		 * atraves do metodo onMessage
		 * 
		 */
		
//		Inicializacao da thread que serve para segurar o mestre ate que o ataque termine
		Thread attackThread = new Thread(attack);
		attackThread.start();
		
		try {
//			Aguarda a thread de ataque terminar..
			attackThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
//		Terminado um ataque, podemos remover ele do mapa de ataques
		synchronized (attacks) {
			attacks.remove(attackNumber);
			System.out.println("The attack [" + attackNumber + "] ended");
		}
		
//		Converte a lista de guess para um array de guess
		Guess[] returnedGuesses = attack.getGuesses()
				.stream()
				.toArray(Guess[]::new);
		
		return returnedGuesses;
	}

	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		
	}
	
}
