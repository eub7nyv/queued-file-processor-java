/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.jbariel.ex;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Entry {

	public enum ExitStatus {
		NORMAL, HELP_SHOWN, UNKNOWN, FILE_NOT_FOUND, FILE_NOT_CLOSED_CORRECTLY,
	}

	private static Logger log = LoggerFactory.getLogger(Entry.class);

	private static EntryManager mgr = EntryManager.instance();

	public static void main(String[] args) {
		mgr.readArgs(args);

		// ACTUALLY DO STUFF

		mgr.closeFileIfOpened();
		exit(ExitStatus.NORMAL);
	}

	public static void exit(ExitStatus status) {
		System.exit((status == null) ? ExitStatus.UNKNOWN.ordinal() : status.ordinal());
	}

	public static BufferedReader getFileToParse() {
		return mgr.getFile();
	}

	static class EntryManager {

		private static final EntryManager entryManager = new EntryManager();

		protected static EntryManager instance() {
			return entryManager;
		}

		private EntryManager() {
			// hide the default constructor
			super();
		}

		private String filename;

		private BufferedReader file;

		private String getFilename() {
			return this.filename;
		}

		private void setFilename(String filename) {
			this.filename = filename;
			tryToLoadFile();
		}

		protected BufferedReader getFile() {
			return this.file;
		}

		protected void tryToLoadFile() {
			try {
				file = new BufferedReader(new FileReader(getFilename()));
			} catch (FileNotFoundException e) {
				log.error("Could not find file with filename: '" + getFilename()
						+ "'!  Check your input and try again.\n");
				printHelpAndExit(ExitStatus.FILE_NOT_FOUND);
			}
			log.info("Successfully loaded file with filename: '" + getFilename() + "'");
		}

		private void readArgs(String[] args) {
			if (args.length == 0) {
				printHelpAndExit();
			}
			int index = 0;
			String tmpFlag, tmpVal;
			do {
				tmpFlag = StringUtils.trimToEmpty(args[index++]);
				tmpVal = (index < (args.length)) ? StringUtils.trimToEmpty(args[index++]) : StringUtils.EMPTY;

				switch (tmpFlag) {
				case "-f":
					setFilename(tmpVal);
					break;
				case "-?":
				case "-H":
				case "-h":
					printHelpAndExit();
					break;
				default:
					log.error("Unknown flag: '" + tmpFlag + "'!!!");
					printHelpAndExit();
				}

			} while (index < args.length);

		}

		protected void closeFileIfOpened() {
			if (null != file) {
				try {
					file.close();
					log.info("Successfully closed file: '" + getFilename() + "'");
				} catch (IOException e) {
					log.error("Failed to close file: '" + getFilename() + "'!!!");
					e.printStackTrace();
					Entry.exit(ExitStatus.FILE_NOT_CLOSED_CORRECTLY);
				}
			}
		}

		private void printHelpAndExit() {
			printHelpAndExit(ExitStatus.HELP_SHOWN);
		}

		private void printHelpAndExit(ExitStatus status) {
			System.out.println(
					"\n===================================== HOW TO USE THIS JAR ====================================="
							+ "\n\tThis jar can be called with switched arguments as follows:"
							+ "\n\n\t\t -f <filename>   => sets the file to process"
							+ "\n\t\t\t THIS FILE MUST BE RELATIVE TO THE CWD - NOT THE JAR FILE"
							+ "\n\n\t\t -h | -H | -?    => prints this help and exits"
							+ "\n\n===============================================================================================\n\n");

			closeFileIfOpened();
			Entry.exit(status);
		}

	}

}
