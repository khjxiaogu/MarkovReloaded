package com.khjxiaogu.markovr;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {
	
	public static void main(String[] args){
		
			

		
		Markov m=new Markov();
		Scanner s=new Scanner(System.in);
		System.out.println("enter help to get help");
		while(true) {
			String sarg=s.nextLine();
			args=sarg.split(" ");
			switch(args[0]) {
			case "train" : m.train(sarg.substring(6),"");break;
			case "trainf" : try {
					m.train(FileUtil.readString(new File(sarg.substring(7))),"");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}break;
			case "reply" : System.out.println(m.fret(sarg.substring(6)));break;
			case "gen": System.out.println(m.genLarge(args[1],Integer.parseInt(args[2])));break;
			case "help":System.out.println("train <text> --Perform Single train\ntrainf <file> --Perform train on file, file must be UTF-8\nreply <text>--Perform write\ngen <text> <length>--Perform large generation");break;
			case "exit":System.exit(0);
			}
		}
	}

}
