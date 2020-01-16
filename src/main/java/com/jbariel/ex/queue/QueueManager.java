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
package com.jbariel.ex.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jbariel.ex.queue.QueueItem.ExecutionResult;

public class QueueManager {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private final int CorePoolSize = 10;

	private final ConcurrentLinkedQueue<QueueItem> queue;

	private final ThreadPoolExecutor executor;

	private final List<QueueItem> failedItems = new ArrayList<>();

	public QueueManager() {
		super();
		queue = new ConcurrentLinkedQueue<>();
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(CorePoolSize);
	}

	public void addToQueue(final QueueItem item) {
		queue.add(item);
		item.setFuture(executor.submit(item.getRunner()));
	}

	public boolean hasQueuedItems() {
		return queue.size() > 0;
	}

	public void processQueue() {
		QueueItem item = queue.peek();
		ExecutionResult res = null;
		while (null != item) {
			if (item.getFuture().isDone()) {

				try {
					res = item.getFuture().get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}

				if (null != res) {
					item = queue.poll();

					switch (res) {
					case FAILURE:
						failedItems.add(item);
						break;
					case SUCCESS:
					case STARTED:
					case ENDED:
						// ignore for now
						break;
					default:
						log.error("How did we get a status of '" + res + "'?");
						break;
					}
				}
			}
			item = queue.peek();
		}
	}

	public List<QueueItem> getFailedItems() {
		return this.failedItems;
	}

}
