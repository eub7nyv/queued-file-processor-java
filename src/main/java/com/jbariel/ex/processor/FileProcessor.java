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
package com.jbariel.ex.processor;

import java.io.BufferedReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jbariel.ex.Entry;
import com.jbariel.ex.Entry.ExitStatus;
import com.jbariel.ex.queue.QueueItem;
import com.jbariel.ex.queue.QueueManager;

public abstract class FileProcessor {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	protected QueueManager queueManager = null;

	public FileProcessor() {
		super();
	}

	public void setQueueManager(final QueueManager mgr) {
		this.queueManager = mgr;
	}

	public void process(final BufferedReader file) {
		String tmpLine = null;
		int lineNum = 1;
		do {
			try {
				tmpLine = file.readLine();
			} catch (IOException e) {
				log.error("Failed to read file at line: " + lineNum);
				e.printStackTrace();
				Entry.exit(ExitStatus.FILE_READ_FAILURE);
			}
			if (null != tmpLine) {
				// processDataLine(tmpLine, lineNum);
				processDataLineTest(tmpLine, lineNum);
				lineNum++;
			} else {
				log.info("Finished processing file...");
				break;
			}
		} while (true);

	}

	protected abstract void processDataLine(final String line, final int lineNumber);

	protected void processDataLineTest(final String line, final int lineNumber) {
		queueManager.addToQueue(new QueueItem().withFileLineNumber(lineNumber).withFileLineAsString(line));
	}

}
