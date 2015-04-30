package com.anand.precompile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class Precompile {
	
	private static final String EMBER_TEMPLATE_START = "Ember.Handlebars.template(";
	private static final String EMBER_TEMPLATE_END = ");";
	private static final String EMEBR_TEMPLATES_START = "Ember.TEMPLATES['";
	private static final String EMEBR_TEMPLATES_END = "']";
	private static final String PRE_COMPILE_FUN = "precompile";
	
	private static final String ARG_HANDLEBARS = "@handlebars";
	private static final String ARG_EMBER_COMPILER = "@emberCompiler";
	private static final String ARG_TEMPLATES = "@templates";
	private static final String ARG_OUTPUT_FILE = "@outputFile";
	
	private static final String HANDLEBARS = "D:\\handlebars.js";
	private static final String EMBER_PRECOMPILER = "D:\\ember-precompiler.js";
	private static final String EMBER_COMPILER = "D:\\ember-compiler.js";	
	
	private static final List<String> ARG_KEYS = new ArrayList<String>();
	static {
		ARG_KEYS.add(Precompile.ARG_TEMPLATES);
	}
	
	private static final List<String> ARG_OPTIONAL_KEYS = new ArrayList<String>();
	static {
		ARG_OPTIONAL_KEYS.add(Precompile.ARG_OUTPUT_FILE);
		ARG_OPTIONAL_KEYS.add(Precompile.ARG_HANDLEBARS);
		ARG_OPTIONAL_KEYS.add(Precompile.ARG_EMBER_COMPILER);
	}	
	
	private static final Map<String, String> ARG_OPTIONAL_FILE = new HashMap<String, String>();
	static {
		ARG_OPTIONAL_FILE.put(Precompile.ARG_HANDLEBARS, Precompile.HANDLEBARS);
		ARG_OPTIONAL_FILE.put(Precompile.ARG_EMBER_COMPILER, Precompile.EMBER_PRECOMPILER);
	}	
	
	private Reader handlebarsReader;
	private Reader emberPrecompilerReader;
	private FileReader emberCompilerReader;
	
	public Precompile() throws FileNotFoundException {
		emberCompilerReader = new FileReader(Precompile.EMBER_COMPILER);
	}
		
	
	public void precompile(String templatesDir, String outputFile) throws Exception {
		System.out.println("Validating template path.");
		File templatesPath = new File(templatesDir);
		if(!templatesPath.exists() || !templatesPath.isDirectory()) {
			System.out.println("Templated directory is not valid. ");
			throw new FileNotFoundException(templatesDir);
		}
		
		System.out.println("Validataion of template path completed.");
		
		System.out.println("Precompilation process started.");
		
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        
        System.out.println("Library files evaluation started.");
        
        engine.eval(handlebarsReader);
        engine.eval(emberPrecompilerReader);
        engine.eval(emberCompilerReader);    
        
        System.out.println("Library files intailized.");
        
        Invocable inv = (Invocable) engine;
        
        System.out.println("Reading hbs files from path specified " + templatesPath.getAbsolutePath());
        List<File> hbsFiles = getHbsFiles(templatesPath, null);
        System.out.println("Hbs files reading completed.");
        
        List<String> precompiledList = new ArrayList<String>();
        StringBuffer precompiledBuffer;
        String hbsContent;
        String templateName;
        System.out.println("Precompiling each hbs file.");
        for(File hbs: hbsFiles) {
        	try {
        		templateName = getTemplateName(hbs, templatesPath.getAbsolutePath());
        		System.out.println("Compiling file " + templateName);
            	hbsContent = Precompile.readFile(hbs.getAbsolutePath());
            	if(null != hbsContent) {
                	precompiledBuffer = new StringBuffer();
                	precompiledBuffer.append(Precompile.EMEBR_TEMPLATES_START);
                	precompiledBuffer.append(templateName);
                	precompiledBuffer.append(Precompile.EMEBR_TEMPLATES_END);
                	precompiledBuffer.append(" = ");
                	precompiledBuffer.append(Precompile.EMBER_TEMPLATE_START);
                	precompiledBuffer.append(inv.invokeFunction(Precompile.PRE_COMPILE_FUN, hbsContent).toString());
                	precompiledBuffer.append(Precompile.EMBER_TEMPLATE_END);
                	precompiledList.add(precompiledBuffer.toString().replaceAll("\\n", ""));
            	}         		
        	} catch(Exception e) {
        		System.out.println("Problem (" + e.getMessage() + ") while precompiling file " + hbs.getName() + ". Trying to precompile next file.");
        		continue;
        	}
        }
        
        System.out.println("Precompiling each hbs file completed.");
        
        loadTemplateFile(outputFile, precompiledList);
        
        System.out.println("Precompilation process ended.");
	}
	
	private static String readFile(String url) throws Exception {
		String content = null;
		Scanner scanner = null;
		try {
			try {
				scanner = new Scanner(new URL(url).openStream(), "UTF-8");
			} catch(MalformedURLException e) {
				scanner = new Scanner(new File(url), "UTF-8");
			}
			content = scanner.useDelimiter("\\A").next();
		} catch (Exception e) {
			throw new Exception();
		} finally {
			if(null != scanner) {
				scanner.close();
			}
		}
		return null != content? content.replaceAll("\\n", ""): content;
	}
	
	private List<File> getHbsFiles(File dir, List<File> files) {
	    if (files == null) {
	    	files = new ArrayList<File>();
	    }
	    String extension;
	    if (!dir.isDirectory()) {
	    	extension = getFileExtension(dir);
	    	if("hbs".equals(extension) || "handlebar".equals(extension) || "handlebars".equals(extension)) {
	    		files.add(dir);
	    	}
	        return files;
	    }

	    for(File file : dir.listFiles()) {
	    	getHbsFiles(file, files);
	    }
	    
	    return files;
	}
	
	private String getTemplateName(File hbs, String templatePath) {
		String hbsName = hbs.getAbsolutePath();
		hbsName = hbsName.substring(0, hbsName.lastIndexOf("."));
		return hbsName.replace(templatePath, "").replaceAll("/", "\\").substring(1);
	}
	
	private String getFileExtension(File file) {
		String fileName = file.getName();
		return fileName.substring(fileName.lastIndexOf(".") + 1);
	}
	
	private void loadTemplateFile(String outputFile, List<String> precompiledList) {
		PrintWriter pWriter = null;
		File output = null;
		try {
			if(null == outputFile) {
				throw new FileNotFoundException();
			}
			output = new File(outputFile);
			pWriter = new PrintWriter(output);
			System.out.println("Writing to outputfile "+ output.getName());
			for(String precompiledString: precompiledList) {
				pWriter.println(precompiledString);
			}
			System.out.println("Output is written into output file.");
			pWriter.flush();
		} catch (FileNotFoundException e) {
			for(String precompiledString: precompiledList) {
				System.out.println(precompiledString);
			}
			pWriter = null;
		} finally {
			if(null != pWriter) {
				pWriter.close();
			}
		}
	}
	
	private static void argsInfo() {
		System.out.println("Arguments");
		System.out.println("=========");
		System.out.println("Mandatory");
		System.out.printf("\t\t%20s:\t%s\n", Precompile.ARG_TEMPLATES, "Path of templates directory.");
		System.out.printf("\t\t%20s:\t%s\n", Precompile.ARG_OUTPUT_FILE, "Path of output file.");
		System.out.println("Optional");
		System.out.printf("\t\t%20s:\t%s\n", Precompile.ARG_HANDLEBARS, "Path of handlebars.js.");
		System.out.printf("\t\t%20s:\t%s\n", Precompile.ARG_EMBER_COMPILER, "Path of ember compiler function js.");
					
		System.exit(0);
	}
	
	

	/**
	 * @param args
	 * @throws ScriptException 
	 * @throws NoSuchMethodException 
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) {
		try {
			
			List<String> arguments = Arrays.asList(args);
			if(arguments.size() < 2) {
				Precompile.argsInfo();
			}
			Map<String, String> argumentMap = new HashMap<String, String>(Precompile.ARG_OPTIONAL_FILE);
			String key;
			String value;
			for(int i = 0; i < arguments.size(); i = i + 2) {
				key = arguments.get(i);
				value = arguments.get(i + 1);
				if(Precompile.ARG_KEYS.contains(key) && null != value) {	
					argumentMap.put(key, value);
				} else if(Precompile.ARG_OPTIONAL_KEYS.contains(key)){
					if(null == value) {
						value = Precompile.ARG_OPTIONAL_FILE.get(key);
					}
					argumentMap.put(key, value);
				} else {
					Precompile.argsInfo();
					break;
				}
			}

			Precompile p = new Precompile();
			p.handlebarsReader = new InputStreamReader(new FileInputStream(new File(argumentMap.get(Precompile.ARG_HANDLEBARS))));
			p.emberPrecompilerReader = new InputStreamReader(new FileInputStream(new File(argumentMap.get(Precompile.ARG_EMBER_COMPILER))));
			p.precompile(argumentMap.get(Precompile.ARG_TEMPLATES), argumentMap.get(Precompile.ARG_OUTPUT_FILE));			
		} catch(Exception e) {
			System.out.println("Problem while precompilation "+ e.getMessage());
		}

	}

}
