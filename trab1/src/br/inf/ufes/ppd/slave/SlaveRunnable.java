package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.DictionaryReader;
import br.inf.ufes.ppd.utils.Partition;

public class SlaveRunnable implements Runnable {

	private String slaveName;
	private UUID slaveKey;

	private DictionaryReader dictionary;

	private Partition partition;

	private byte[] cipherText;
	private byte[] knownText;
	private int attackNumber;
	private SlaveManager callbackInterface;

	public SlaveRunnable(String slaveName, UUID slaveKey) {
		this.slaveName = slaveName;
		this.slaveKey = slaveKey;
	}

	public String getSlaveName() {
		return slaveName;
	}

	public UUID getSlaveKey() {
		return slaveKey;
	}

	public void setDictionary(DictionaryReader dictionary) {
		this.dictionary = dictionary;
	}

	public DictionaryReader getDictionary() {
		return dictionary;
	}

	public Partition getPartition() {
		return partition;
	}

	public void setPartition(Partition partition) {
		this.partition = partition;
	}

	public void setSubAttackParameters(byte[] cipherText, byte[] knownText, int attackNumber,
			SlaveManager callbackInterface) {
		this.cipherText = cipherText;
		this.knownText = knownText;
		this.attackNumber = attackNumber;
		this.callbackInterface = callbackInterface;
	}

	@Override
	public void run() {
//		Abertura do utilitario de leitura de dicionario com fechamento automatico
		SlaveCheckpointAssistant checkPointAssistant = new SlaveCheckpointAssistant(slaveName, slaveKey,
				attackNumber, callbackInterface);

		Thread checkPointAssistantThread = new Thread(checkPointAssistant);
		checkPointAssistantThread.start();

//		Enquanto houver alguma coisa para ler no dicionario...
		while (dictionary.ready()) {
			String key = dictionary.readLine();

			SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "Blowfish");

			try {
				Cipher cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

				byte[] decrypted = cipher.doFinal(cipherText);

				System.out.println(slaveName + " -> [" + dictionary.getLineNumber() + ", " + key + "]: Gotcha!!");

				Guess guess = new Guess();
				guess.setKey(key);
				guess.setMessage(decrypted);

				String decryptedStr = new String(decrypted);
				String knownTextStr = new String(knownText);

				if (decryptedStr.contains(knownTextStr)) {
					try {
						callbackInterface.foundGuess(slaveKey, attackNumber, dictionary.getLineNumber() - 1, guess);
					} catch (RemoteException e) {
//						Houve algum problema com o mestre durante o ataque.
//						O escravo deve finalizar o subAttack e continuar tentando fazer contato com
//						um novo mestre.
						e.printStackTrace();
					}
				}

				checkPointAssistant.setCurrentIndex(dictionary.getLineNumber() - 1);
			} catch (BadPaddingException e) {
//				Chave errada, so atualiza o index
//				notification("[" + dictionary.getLineNumber() + ", " + key + "]: Invalid Key");
				checkPointAssistant.setCurrentIndex(dictionary.getLineNumber() - 1);
			} catch (InvalidKeyException | IllegalBlockSizeException e) {
//				Chave mal formatada ou .cipher nao multiplo de 8
				e.printStackTrace();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//				Se rolarem essas excecoes, a Terra ja estara colidindo com o Sol
				e.printStackTrace();
			}
		}

		checkPointAssistant.workFinished();
	}

}