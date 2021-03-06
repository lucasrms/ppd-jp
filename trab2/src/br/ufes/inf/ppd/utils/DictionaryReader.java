package br.ufes.inf.ppd.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Lucas Ribeiro Mendes Silva
 *
 */
public class DictionaryReader implements Iterable<String> {

	private List<String> lines;
	
	private int start;
	private int end;
	private int currentLineNumber;
	
	/**
	 * Utilitario de leitura do dicionario
	 * 
	 * @param caminho do arquivo
	 * @throws IOException
	 */
	public DictionaryReader(String filePath) {
		File file = new File(filePath);
		
		if (file.exists()) {
			try {
				lines = Files.readAllLines(file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Arquivo de dicionario " + filePath + " nao foi encontrado");
			lines = new ArrayList<>();
		}
		
		start = 0;
		end = lines.size();
		currentLineNumber = 0;
	}
	
	/**
	 * Copia de dictionary reader (sem salvar o estado do ponteiro)
	 * 
	 * @param dictionaryReader
	 */
	public DictionaryReader(DictionaryReader dictionaryReader) {
		this.lines = new ArrayList<String>(dictionaryReader.lines);
		this.start = 0;
		this.end = dictionaryReader.countAllLines();
		this.currentLineNumber = 0;
	}
	
	/**
	 * Copia de uma lista
	 * 
	 * @param lines
	 */
	public DictionaryReader(List<String> lines) {
		this.lines = new ArrayList<String>(lines);
		this.start = 0;
		this.end = lines.size();
		this.currentLineNumber = 0;
	}
	
	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	/**
	 * Estabelece um range de leitura entre as linhas do arquivo
	 * 
	 * @param index de inicio
	 * @param index de fim
	 * @throws IndexOutOfBoundsException
	 */
	public void setRange(int start, int end) throws IndexOutOfBoundsException {
		if ((start < 0) || (end > lines.size()))
			throw new IndexOutOfBoundsException();
		
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Remove o range estabelecido entre as linhas do arquivo
	 */
	public void unsetRange() {
		start = 0;
		end = lines.size();
	}
	
	/**
	 * Realiza a leitura da proxima linha no buffer
	 * 
	 * @return proxima linha do buffer
	 */
	public String readLine() {
		String line = lines.get(currentLineNumber);
		currentLineNumber++;
		
		return line;
	}
	
	/**
	 * Realiza a leitura de uma linha especifica
	 * 
	 * @param index da linha desejada
	 * @return linha desejada
	 * @throws IndexOutOfBoundsException
	 */
	public String readLine(int index) throws IndexOutOfBoundsException {
		if ((index < start) || (index >= end))
			throw new IndexOutOfBoundsException();
		
		return lines.get(index);
	}
	
	/**
	 * Realiza a leitura de um numero de linhas dado um range
	 * 
	 * @param tamanho do buffer
	 * @return lista das linhas dentro do range especificado
	 */
	public List<String> readLines(int buffer) {
		List<String> linesRead = new ArrayList<String>();
		
		for (int i = 0; i < buffer; i++) {
			if (ready()) {
				linesRead.add(readLine());
			} else {
				break;
			}
		}
		
		return linesRead;
	}
	
	/**
	 * Verifica se um dado index existe no dicionario
	 * 
	 * @param index
	 * @return
	 */
	public boolean hasIndex(int index) {
		return ((index >= start) && (index < end));
	}
	
	/**
	 * Avanca o ponteiro dado um intervalo, nunca sai do intervalo estabelecido
	 * 
	 * @param tamanho do salto do ponteiro
	 * @throws IndexOutOfBoundsException
	 */
	public void seek(int buffer) {
		int pointer = currentLineNumber + buffer;
		
		if ((pointer >= start) && (pointer < end)) {
			currentLineNumber = pointer;
		} else {
			if (pointer < start) {
				currentLineNumber = start;
			} else {
				currentLineNumber = end;
			}
		}
	}
	
	/**
	 * Verifica se ainda existe linhas para serem lidas
	 * 
	 * @return pode ler a proxima linha ou nao
	 */
	public boolean ready() {
		return (currentLineNumber >= start) && (currentLineNumber < end);
	}
	
	/**
	 * Contabiliza todas as linhas dentro do range estabelecido, de start ate end
	 * 
	 * @return numero total de linhas range estabelicido
	 */
	public int countLines() {
		return end - start;
	}
	
	/**
	 * Contabiliza todas as linhas do arquivo original, do primeiro ao ultimo byte
	 * 
	 * @return numero total de linhas no arquivo original
	 */
	public int countAllLines() {
		return lines.size();
	}
	
	/**
	 * Retorna o ponteiro para a primeira linha do arquivo
	 */
	public void rewind() {
		currentLineNumber = start;
	}
	
	/**
	 * Retorna a linha a qual o ponteiro se encontra
	 * 
	 * @return linha do ponteiro
	 */
	public int getLineNumber() {
		return currentLineNumber;
	}
	
	/**
	 * Fragmenta um dicionario em N particoes
	 * 
	 * @param numberOfPartitions
	 * @return lista de particoes
	 */
	public Set<Partition> toPartitions(int numberOfPartitions) {
		Set<Partition> partitions = new HashSet<Partition>();

		int partitionLength = countLines() / numberOfPartitions;
		int partitionLeftovers = countLines() % numberOfPartitions;

		while (ready()) {
			int min = getLineNumber();

			if (partitionLeftovers > 0) {
				seek(partitionLength + 1);
				partitionLeftovers--;
			} else {
				seek(partitionLength);
			}

			int max = getLineNumber();

			partitions.add(new Partition(min, max));
		}

		rewind();
		
		return partitions;
	}
	
	/**
	 * Fragmenta um dicionario em N particoes de tamanho numberOfWords cada
	 * 
	 * @param numberOfWords
	 * @return
	 */
	public Set<Partition> chunk(int numberOfWords) {
		Set<Partition> partitions = new HashSet<Partition>();
		
		while (ready()) {
			int min = getLineNumber();
			seek(numberOfWords);
			int max = getLineNumber();
			
			partitions.add(new Partition(min, max));
		}
		
		rewind();
		
		return partitions;
	}
	
	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {

			@Override
			public boolean hasNext() {
				return ready();
			}

			@Override
			public String next() {
				return readLine();
			}
			
		};
	}

}
