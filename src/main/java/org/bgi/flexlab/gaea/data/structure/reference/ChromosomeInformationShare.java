/**
 * Copyright (c) 2011, BGI and/or its affiliates. All rights reserved.
 * BGI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.bgi.flexlab.gaea.data.structure.reference;

import java.io.IOException;

import org.bgi.flexlab.gaea.exception.OutOfBoundException;
import org.bgi.flexlab.gaea.util.SystemConfiguration;

/**
 * 染色体信息共享内存
 * 
 * @author ZhangYong
 *
 */
public class ChromosomeInformationShare extends InformationShare {

	private static int CAPACITY = Byte.SIZE / 4;

	/**
	 * 染色体名称
	 */
	private String chrName;

	/**
	 * 染色体对应参考基因组长度
	 */
	private int length;

	/**
	 * 二进制格式的序列。 每4个bit表示一个碱基的信息: 第一位表示dbSNPstatus 第二位表示N 最后两位表示碱基类型，其中A: 00, C:
	 * 01, T: 10, G:11 每个long类型变量可以存储16个碱基的信息
	 */
	// private MappedByteBuffer[] refSeq; // reference seq map

	/**
	 * dbSNP 信息
	 */
	private ChromosomeDbSNPShare dbsnpInfo;

	/**
	 * @return the chrName
	 */
	public String getChromosomeName() {
		return chrName;
	}

	/**
	 * @param chrName
	 *            the chrName to set
	 */
	public void setChromosomeName(String chrName) {
		this.chrName = chrName;
	}

	/**
	 * 设置染色体对应参考基因组长度
	 * 
	 * @param chrLen
	 *            染色体长度
	 */
	public void setLength(int chrLen) {
		length = chrLen;
	}

	/**
	 * 获取染色体对应参考基因组长度
	 */
	public int getLength() {
		return length;
	}

	/**
	 * 映射一条染色体文件到内存
	 * 
	 * @param chr
	 *            染色体文件名
	 * @throws IOException
	 */
	public void loadChromosome(String chr) throws IOException {
		loadInformation(chr);
	}

	/**
	 * 映射dbSNP信息到内存 有dbsnp信息才初始化dbsnpInfo
	 * 
	 * @throws IOException
	 * @param dbsnpPath
	 *            dbSNP path
	 * @return 0
	 */
	public void loadDbSNP(String dbsnpPath, String indexPath, int size)
			throws IOException {
		dbsnpInfo = new ChromosomeDbSNPShare(dbsnpPath, indexPath, size);
	}

	/**
	 * 获取dbsnp信息
	 * 
	 * @param
	 * @return ChromosomeDbSNPShare
	 */
	public ChromosomeDbSNPShare getDbsnpInfo() {
		return dbsnpInfo;
	}

	public byte[] getBytes(int start, int end) {
		if (start > length)
			throw new OutOfBoundException(start, length);

		byte[] bases;

		int posi = start / CAPACITY;
		int pose;
		if (end >= length) {
			pose = (length - 1) / CAPACITY;
		} else {
			pose = end / CAPACITY;
		}
		bases = new byte[pose - posi + 1];
		byteBuffer[0].position(posi);
		byteBuffer[0].get(bases, 0, pose - posi + 1);
		byteBuffer[0].position(0);

		return bases;
	}

	/**
	 * get base from reference position
	 * 
	 * @param position
	 * @return base
	 */
	public byte getBinaryBase(int pos) {
		return getBytes(pos, pos)[0];
	}

	/**
	 * 获取碱基
	 */
	public char getBase(int pos) {
		return SystemConfiguration.getFastaAbb(getBinaryBase(pos));
	}

	/**
	 * 获取染色体序列
	 * 
	 * @param start
	 *            从0开始
	 * @param end
	 * @return String 序列
	 */
	public String getBaseSequence(int start, int end) {
		StringBuffer seq = new StringBuffer();
		byte[] bases = getBytes(start, end);

		if ((start & 0x1) == 0) {
			seq.append(SystemConfiguration.getFastaAbb(bases[0] & 0x0f));
			seq.append(SystemConfiguration.getFastaAbb((bases[0] >> 4) & 0x0f));
		} else {
			seq.append(SystemConfiguration.getFastaAbb((bases[0] >> 4) & 0x0f));
		}
		// 取一个位点
		if (start == end) {
			return seq.toString();
		}
		for (int i = 1; i < bases.length - 1; i++) {
			seq.append(SystemConfiguration.getFastaAbb(bases[i] & 0x0f));
			seq.append(SystemConfiguration.getFastaAbb((bases[i] >> 4) & 0x0f));
		}
		if ((end & 0x1) == 0) {
			seq.append(SystemConfiguration
					.getFastaAbb(bases[bases.length - 1] & 0x0f));
		} else {
			seq.append(SystemConfiguration
					.getFastaAbb(bases[bases.length - 1] & 0x0f));
			seq.append(SystemConfiguration
					.getFastaAbb((bases[bases.length - 1] >> 4) & 0x0f));
		}
		return seq.toString();
	}

	/**
	 * 转换byte数组为字符串
	 * 
	 * @param b
	 *            输入byte数组
	 * @return 返回相应的字符串
	 */
	public static String bytes2String(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length);
		for (byte each : b) {
			sb.append((char) each);
		}
		return sb.toString();
	}

	/**
	 * byte转换为double类型
	 */
	public static double byteToDouble(byte[] b, int i) {
		long l = 0;
		for (int j = 0; j < 8; j++) {
			l |= (((long) (b[i + j] & 0xff)) << (8 * j));
		}
		return Double.longBitsToDouble(l);
	}
}
