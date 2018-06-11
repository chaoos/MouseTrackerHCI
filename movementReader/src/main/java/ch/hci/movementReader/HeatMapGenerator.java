package ch.hci.movementReader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
 //TODO clean up code
public class HeatMapGenerator {
	
	private static final double widthRatio1080 = 2560d/1980d;
	private static final double heightRatio1080 = 1440d/1080d;
	
	private static final double windowsZoomRatio = 2d;
	
	private int pageX = 2560;
	private int pageY = 1440;
	private int boxRadius = 5;
	private int[][] boxes = new int[pageX/boxRadius][pageY/boxRadius]; 
	private String desiredUrl = "https://www.facebook.com/";
	private int maxValue = 0;
	
	public HeatMapGenerator(int pageX, int pageY, int boxRadius, String url) {
		this.pageX = pageX;
		this.pageY = pageY;
		this.boxRadius = boxRadius;
		this.desiredUrl = url;
		
	}
	public HeatMapGenerator() {};
	
	private void putPixelInBox(int x, int y, double scaleX, double scaleY) {
		x = (int) Math.round(x * scaleX);
		y = (int) Math.round(y * scaleY);
		
		int boxesX = (int)Math.ceil(x / boxRadius);
		int boxesY = (int)Math.ceil(y / boxRadius);
		try {
		boxes[boxesX][boxesY]++;
			if(boxes[boxesX][boxesY] > maxValue) {
				maxValue = boxes[boxesX][boxesY];
			}
		}catch(ArrayIndexOutOfBoundsException ex) {
			//scrolled down
		}
	}
	
	private void readFile(Path path) throws IOException {
		try{
		System.out.println("scanning "+path.toString()+" for pattern: "+desiredUrl);
		double scaleX = windowsZoomRatio;
		double scaleY = windowsZoomRatio;
		if(path.getFileName().toFile().getName().contains("_1080p")) {
			scaleX = widthRatio1080;
			scaleY = heightRatio1080;
		}
		String url = "";
		List<String> lines = Files.readAllLines(path, Charset.forName("UTF-8"));
		for (String line : lines) {
			String[] lineContents = line.split("\\s+");
			if (lineContents[0].equals("move-->") && url.matches(desiredUrl) ) {
				putPixelInBox(Integer.parseInt(lineContents[2]), Integer.parseInt(lineContents[3]),scaleX,scaleY);
			} else if (lineContents[0].equals("load-->")) {
				url = lineContents[2];
			}
		}
		}catch(Exception ex) {
			ex.printStackTrace();
			System.out.println("Error while reading "+path.toString());
		}
	}

	public List<HeatPoint> generateHeatPoints() {
		List<HeatPoint> heatPoints = new ArrayList<HeatPoint>();
		for (int i = 0; i< pageX/boxRadius; i++) {
			for (int j = 0; j< pageY/boxRadius; j++) {
				int value = boxes[i][j];
				if(value != 0) {
					for(int k = 0; k < boxRadius; k++) {
						for(int l = 0; l < boxRadius; l++) {
							heatPoints.add(new HeatPoint(boxRadius*i+k,boxRadius*j+l,value));
						}
					}
				}
			}
		}
		return heatPoints;
	}
	private void processDirectory(File directory) throws IOException {
		for (File file : directory.listFiles(new FileFilter() {
			
			public boolean accept(File pathname) {
				return !pathname.isDirectory();
			}
		})) {
			readFile(file.toPath());
		}
	}

	public void process(String path) throws IOException {
		File fileOrDir = new File(path);
		if (fileOrDir.isDirectory()) {
			processDirectory(fileOrDir);
		} else {
			readFile(fileOrDir.toPath());
		}
		System.out.println(maxValue);
	}
	
	public static void main(String[] args) {
		try {
			if(args != null) {
				HeatMapGenerator gen = new HeatMapGenerator(
						Integer.parseInt(args[1]), 
						Integer.parseInt(args[2]), 
						Integer.parseInt(args[3]), 
						args[0]);
				
				gen.process(args[4]);
				List<HeatPoint> heatPoints = gen.generateHeatPoints();
				ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
				mapper.writeValue(new File(args[5]), heatPoints);
			}else {
				HeatMapGenerator gen = new HeatMapGenerator();
				gen.process("C://Users/Yannick/Desktop/mousetracking_logs/");
				List<HeatPoint> heatPoints = gen.generateHeatPoints();
				ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
				mapper.writeValue(new File("heat.json"), heatPoints);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
