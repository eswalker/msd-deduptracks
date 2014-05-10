package com.eswalker.msd.DedupTrackList;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;


public class DedupTrackList extends Configured implements Tool{
	public static class HMapper extends Mapper<LongWritable, Text, Text, Text> {	     
		
		private static Text outputKey = new Text();
	    private static Text outputValue = new Text();
	   
	   
	    @Override
	    protected final void setup(final Context context) throws IOException, InterruptedException {
	  
	    }

		@Override
		/**
		 * DedupTrackList mapper
		 * 
		 * Function = key by artisttitle
		 * 
		 * In Value = track_id|lastfm_id|artist|title|num_tags|tag1,score1|tag2,score2|...
		 * 
		 * Out Key = artisttitle
		 * Out Value = In Value
		 */
		public final void map(final LongWritable key, final Text value, Context context) throws IOException, InterruptedException {
			
			String data[] = value.toString().split("\\|");
			String artistAndTitle = data[2]+data[3];
			outputKey.set(artistAndTitle);
			outputValue.set(value.toString());
			context.write(outputKey, outputValue);
			
		}
		
	}

	
	public static class HReducer extends Reducer<Text, Text, NullWritable, Text> {
		private static NullWritable nullKey = NullWritable.get();
        private static Text outputValue = new Text();
        
        @Override
       /***
        * DedupTrackList reducer
        * 
        * Function = dedup values
        * 
        * In key = artist|title
        * In values = Line
        * 
        * Out value = Deduped Line
        *
        */
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        	
        	String lineWithMostTags = "";
        	int maxTags = Integer.MIN_VALUE;
        	        	
        	for (Text t : values) {
        		String[] data = t.toString().split("\\|");
        		int numTags = Integer.parseInt(data[4]);
        		if (numTags > maxTags) { // update max
        			maxTags = numTags;
        			lineWithMostTags = t.toString();
        		}
        	}
        	outputValue.set(lineWithMostTags);
        	context.write(nullKey, outputValue);
        	
        }
        
    }

    /**
     * Sets up job, input and output.
     * 
     * @param args
     *            inputPath outputPath
     * @throws Exception
     */
	public int run(String[] args) throws Exception {

       Configuration conf = getConf();
        
       Job job = new Job(conf);
       job.setJarByClass(DedupTrackList.class);
       job.setJobName("DedupTrackList");
       
       job.setInputFormatClass(TextInputFormat.class);
       
       TextInputFormat.addInputPaths(job, args[0]);
       TextOutputFormat.setOutputPath(job, new Path(args[1]));

       job.setMapperClass(HMapper.class);
       job.setReducerClass(HReducer.class);

       job.setMapOutputKeyClass(Text.class);
       job.setMapOutputValueClass(Text.class);
       job.setOutputKeyClass(NullWritable.class);
       job.setOutputValueClass(Text.class);

       return job.waitForCompletion(true) ? 0 : 1;
    }
	
    public static void main(String args[]) throws Exception {
    	
        if (args.length < 2) {
            System.out.println("Usage: DedupTrackList <input dirs> <output dir>");
            System.exit(-1);
        }
 
        int result = ToolRunner.run(new DedupTrackList(), args);
        System.exit(result);
        
   }


}

