/*******************************************************************************
 * Copyright (c) 2017, BGI-Shenzhen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.bgi.flexlab.gaea.tools.annotator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.bgi.flexlab.gaea.data.structure.reference.ReferenceShare;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPOutputStream;

public class VariantAnnotation extends Configured implements Tool{
	
	Parameter parameter=null;
	
	public VariantAnnotation(){}
	
	public VariantAnnotation(Parameter parameter)
	{
		this.parameter=parameter;
	}
	
	/**
	 * 利用Parameter对象初始化Configuration对象
	 * 
	 * @param conf
	 * @param parameter
	 */
	private static void setConfiguration(Configuration conf, Parameter parameter) {
		//set reference
		conf.set("inputFilePath", parameter.getInputFilePath());
		conf.set("reference", parameter.getReferenceSequencePath());
		conf.set("configFile", parameter.getConfigFile());
		conf.set("outputType", parameter.getOutputType());
		conf.setBoolean("cacheref", parameter.isCachedRef());
		conf.setBoolean("verbose", parameter.isVerbose());
		conf.setBoolean("debug", parameter.isDebug());
	}

	@Override
	public int run(String[] arg0) throws Exception {
		Configuration conf = new Configuration();
		parameter = new Parameter(arg0);
		setConfiguration(conf, parameter);
		Job job = Job.getInstance(conf, "GaeaAnnotator");
		
		if(parameter.isCachedRef()){
			System.err.println("--------- isCachedRef --------");
			ReferenceShare.distributeCache(parameter.getReferenceSequencePath(), job);
		}
		
		job.setNumReduceTasks(0);
		job.setJarByClass(VariantAnnotation.class);
		job.setMapperClass(VariantAnnotationMapper.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setInputFormatClass(MNLineInputFormat.class);
		
		MNLineInputFormat.addInputPath(job, new Path(parameter.getInputFilePath()));
		MNLineInputFormat.setMinNumLinesToSplit(job,1000); //按行处理的最小单位
		MNLineInputFormat.setMapperNum(job, parameter.getMapperNum()); 
		Path partTmp = new Path(parameter.getTmpPath());
		
		FileOutputFormat.setOutputPath(job, partTmp);
		if (job.waitForCompletion(true)) {
			GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(parameter.getOutputPath()));
			final FileStatus[] parts = partTmp.getFileSystem(conf).globStatus( new Path(parameter.getTmpPath()+"/part" + "-*-[0-9][0-9][0-9][0-9][0-9]*"));
            boolean writeHeader = true;
			for (FileStatus p : parts) {
            	FSDataInputStream dis = p.getPath().getFileSystem(conf).open(p.getPath());
            	BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
            	String line;
            	while ((line = reader.readLine()) != null) {
            		if(line.startsWith("#")){
            			if (writeHeader){
            				os.write(line.getBytes());
                    		os.write('\n');
            				writeHeader = false;
            			}
            			continue;
            		}
            		os.write(line.getBytes());
            		os.write('\n');
				}
            }
			os.close();
			
			partTmp.getFileSystem(conf).delete(partTmp, true);
			
			return 0;
		}else {
			return 1;
		}
	}

}
