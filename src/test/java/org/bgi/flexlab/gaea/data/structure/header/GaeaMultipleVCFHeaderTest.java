package org.bgi.flexlab.gaea.data.structure.header;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.bgi.flexlab.gaea.data.structure.header.GaeaMultipleVCFHeader;
import org.junit.Before;

public class GaeaMultipleVCFHeaderTest {
	GaeaMultipleVCFHeader gmv;
	@Before
	public void setup(){
		gmv = new GaeaMultipleVCFHeader();
	}

//	@Test
	public void mergeHeader(){
		
		String output = "testOutput";
		Configuration conf = new Configuration(false);
		conf.set("fs.default.name", "file:///");
		boolean distributeCacheHeader = false;
		Path inputPath = new Path("file:///ifs4/ISDC_BD/huweipeng/data/testVCF");
		gmv.mergeHeader(inputPath , output, conf, distributeCacheHeader );
		gmv.loadVcfHeader(output);
		for(String file:gmv.getFileName2ID().keySet()){
			System.out.println(file);
		}
	}
}
