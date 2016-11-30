/**
 * Copyright (c) 2011, BGI and/or its affiliates. All rights reserved.
 * BGI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.bgi.flexlab.gaea.data.structure.reference;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * dbSNP信息共享内存
 * 
 * @author ZhangYong
 *
 */
public class ChromosomeDbSNPShare extends InformationShare {
	/**
	 * dbSNP记录条数
	 */
	private int dbsnpRecordCount;

	/**
	 * dbSNP索引数组
	 */
	private int[] dbsnpIndex;

	/**
	 * 存储dbSNP信息的Map文件
	 */
	//private MappedByteBuffer[] dbsnpBuffer;

	/**
	 * dbSNP占用存储空间大小
	 */
	private int dbsnpSize;

	/**
	 * 构造方法
	 * 
	 * @param dbsnpPath
	 * @param indexPath
	 * @param dbnum
	 */
	public ChromosomeDbSNPShare(String dbsnpPath, String indexPath, int dbnum) {
		dbsnpRecordCount = dbnum;
		dbsnpIndex = new int[dbsnpRecordCount];
		dbsnpSize = (Byte.SIZE + Float.SIZE) / 8;
		try {
			loadDbSNP(dbsnpPath);
			loadDbSNPIndex(indexPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 映射dbSNP信息到内存
	 * 
	 * @throws IOException
	 * @param dbsnpPath
	 */
	public void loadDbSNP(String dbsnpPath) throws IOException {
		loadInformation(dbsnpPath);
	}

	/**
	 * 读取dbsnp index文件
	 */
	public void loadDbSNPIndex(String indexPath) throws IOException {
		DataInputStream in = new DataInputStream(new BufferedInputStream(
				new FileInputStream(indexPath)));
		int i = 0;
		while (i < dbsnpRecordCount) {
			dbsnpIndex[i] = in.readInt();
			i++;
		}
		in.close();
	}

	/**
	 * 二分法查找
	 */
	public int binarySearch(int start, int end, int[] array, int value) {
		int i, j, k, index = -1;
		i = start;
		j = end;
		while (i <= j) {
			k = (i + j) >> 1;
			if (value > array[k]) {
				i = k + 1;
			}
			if (value < array[k]) {
				j = k - 1;
			}
			if (value == array[k]) {
				index = k;
				break;
			}
		}
		return index;
	}

	/**
	 * 找到dbSNP的index值
	 */
	public int findDbSNPIndex(int pos) {
		int index = 0;
		index = binarySearch(0, dbsnpRecordCount - 1, dbsnpIndex, pos);
		return index;
	}

	/**
	 * 按位点坐标寻找SNP
	 * 
	 * @param pos
	 * @return SNPInfo
	 */
	public SNPInformation findSNP(int pos) {
		byte snpBasicInfo;
		float alleleFreq;
		SNPInformation snpinfo = new SNPInformation();

		int index = findDbSNPIndex(pos);
		if (index == -1) {
			return null;
		}

		byteBuffer[0].position(index * dbsnpSize);
		snpBasicInfo = byteBuffer[0].get();
		alleleFreq = byteBuffer[0].getFloat();
		byteBuffer[0].position(0);

		snpinfo.setSnpBasicInformation(snpBasicInfo);
		snpinfo.setAlleleFreq(alleleFreq);

		return snpinfo;
	}
}
