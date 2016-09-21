/**
 * Copyright (c) 2011, BGI and/or its affiliates. All rights reserved.
 * BGI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.bgi.flexlab.gaea.data.structure.reference;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.bgi.flexlab.gaea.util.SystemConfig;

/**
 * 染色体信息
 * 
 * @author ZhangYong, ZhangZhi
 *
 */
public class ChromosomeInformation {
	/**
	 * 染色体对应参考基因组长度
	 */
	private int length;

	/**
	 * 二进制格式的序列。 每4个bit表示一个碱基的信息: 第一位表示IsDbSNP，其中1表示dbSNP数据库已知变异位点，0则相反
	 * 第二位表示IsN，其中1表示N，0则相反 最后两位表示碱基类型，其中A: 00, C: 01, T: 10, G:11
	 * 每个long类型变量可以存储16个碱基的信息
	 */
	private byte[] binarySeq;

	/**
	 * 获取染色体对应参考基因组长度
	 */
	public int getLength() {
		return length;
	}

	/**
	 * 二进制化参考基因组序列
	 * 
	 * @param sequence
	 *            文本碱基序列
	 */
	public void setBinarySequence(String sequence) {
		// 为类成员变量length赋值
		length = sequence.length();
		int capacity = length;
		if (length % SystemConfig.getCapacity() != 0)
			capacity++;

		// 为类成员变量binarySeq分配内存空间
		binarySeq = new byte[capacity];

		// 处理碱基序列的每一个字符，初始化binarySeq数组。每个碱基用4bit表示。
		for (int i = 0; i < sequence.length(); i++) {
			binarySeq[i / SystemConfig.getCapacity()] |= ((((byte) sequence
					.charAt(i) >> 1) & 7) << (i % SystemConfig.getCapacity() * 4));
		}
	}

	/**
	 * 向二进制ref中添加dbSNP flag
	 * 
	 * @param pos
	 *            有dbSNP信息的位点
	 */
	public void insertSnpInformation(int pos) {
		byte intValue = 1;
		binarySeq[pos / SystemConfig.getCapacity()] |= (intValue << (pos
				% SystemConfig.getCapacity() * 4 + 3));
	}

	/**
	 * 输出编码的二进制碱基信息
	 * 
	 * @param outPath
	 *            输出路径
	 * @throws IOException
	 */
	public void outputChrInformation(String outPath) throws IOException {
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(outPath)));
		out.write(binarySeq);
		out.close();
	}
}
