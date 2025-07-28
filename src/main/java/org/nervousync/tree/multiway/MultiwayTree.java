/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nervousync.tree.multiway;

import jakarta.annotation.Nonnull;
import org.nervousync.enumerations.tree.RecursionType;
import org.nervousync.utils.ObjectUtils;

import java.util.*;

/**
 * <h2 class="en-US">Multi-node Tree</h2>
 * <h2 class="zh-CN">多叉树</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 3, 2017 17:15:52 $
 */
public final class MultiwayTree<T> {

	/**
	 * <span class="en-US">Node value</span>
	 * <span class="zh-CN">节点值</span>
	 */
	private T nodeValue;
	/**
	 * <span class="en-US">Parent node</span>
	 * <span class="zh-CN">父节点</span>
	 */
	private MultiwayTree<T> parentNode;
	/**
	 * <span class="en-US">Child node list</span>
	 * <span class="zh-CN">子节点列表</span>
	 */
	private final List<MultiwayTree<T>> childNodes;

	/**
	 * <h3 class="en-US">Constructor method for the multi-node tree</h3>
	 * <h3 class="zh-CN">多叉树构造方法</h3>
	 *
	 * @param nodeValue <span class="en-US">Node value</span>
	 *                  <span class="zh-CN">节点值</span>
	 */
	public MultiwayTree(final T nodeValue) {
		this(nodeValue, null);
	}

	/**
	 * <h3 class="en-US">Constructor method for the multi-node tree</h3>
	 * <h3 class="zh-CN">多叉树构造方法</h3>
	 *
	 * @param nodeValue  <span class="en-US">Node value</span>
	 *                   <span class="zh-CN">节点值</span>
	 * @param parentNode <span class="en-US">Parent node</span>
	 *                   <span class="zh-CN">父节点</span>
	 */
	public MultiwayTree(final T nodeValue, final MultiwayTree<T> parentNode) {
		this.nodeValue = nodeValue;
		this.parentNode = parentNode;
		this.childNodes = new ArrayList<>();
	}

	/**
	 * <h3 class="en-US">Getter method for node value</h3>
	 * <h3 class="zh-CN">节点值的Getter方法</h3>
	 *
	 * @return <span class="en-US">Node value</span>
	 * <span class="zh-CN">节点值</span>
	 */
	public T getNodeValue() {
		return nodeValue;
	}

	/**
	 * <h3 class="en-US">Setter method for node value</h3>
	 * <h3 class="zh-CN">节点值的Setter方法</h3>
	 *
	 * @param nodeValue <span class="en-US">Node value</span>
	 *                  <span class="zh-CN">节点值</span>
	 */
	public void setNodeValue(final T nodeValue) {
		this.nodeValue = nodeValue;
	}

	/**
	 * <h3 class="en-US">Getter method for parent node</h3>
	 * <h3 class="zh-CN">父节点的Getter方法</h3>
	 *
	 * @return <span class="en-US">Parent node</span>
	 * <span class="zh-CN">父节点</span>
	 */
	public MultiwayTree<T> getParentNode() {
		return parentNode;
	}

	/**
	 * <h3 class="en-US">Setter method for parent node</h3>
	 * <h3 class="zh-CN">父节点的Setter方法</h3>
	 *
	 * @param parentNode <span class="en-US">Parent node</span>
	 *                   <span class="zh-CN">父节点</span>
	 */
	public void setParentNode(final MultiwayTree<T> parentNode) {
		this.parentNode = parentNode;
	}

	/**
	 * <h3 class="en-US">Getter method for the child node list</h3>
	 * <h3 class="zh-CN">子节点列表的Getter方法</h3>
	 *
	 * @return <span class="en-US">Child node list</span>
	 * <span class="zh-CN">子节点列表</span>
	 */
	public List<MultiwayTree<T>> getChildNodes() {
		return childNodes;
	}

	/**
	 * <h3 class="en-US">Add child node</h3>
	 * <h3 class="zh-CN">添加子节点</h3>
	 *
	 * @param nodeValue <span class="en-US">Node value</span>
	 *                  <span class="zh-CN">节点值</span>
	 */
	public void addChild(final T nodeValue) {
		if (this.childNodes.stream().noneMatch(treeNode ->
				ObjectUtils.nullSafeEquals(treeNode.getNodeValue(), nodeValue))) {
			this.childNodes.add(new MultiwayTree<>(nodeValue, this));
		}
	}

	/**
	 * <h3 class="en-US">Add child node</h3>
	 * <h3 class="zh-CN">添加子节点</h3>
	 *
	 * @param parentValue <span class="en-US">Parent node value</span>
	 *                    <span class="zh-CN">父节点值</span>
	 * @param nodeValue   <span class="en-US">Node value</span>
	 *                    <span class="zh-CN">节点值</span>
	 */
	public boolean addChild(final T parentValue, final T nodeValue) {
		if (ObjectUtils.nullSafeEquals(this.nodeValue, parentValue)) {
			this.addChild(nodeValue);
			return Boolean.TRUE;
		}
		return this.childNodes.stream().anyMatch(treeNode -> treeNode.addChild(parentValue, nodeValue));
	}

	/**
	 * <h3 class="en-US">Add child node</h3>
	 * <h3 class="zh-CN">添加子节点</h3>
	 *
	 * @param treeNode <span class="en-US">Child node instance</span>
	 *                 <span class="zh-CN">子节点</span>
	 */
	public void addChild(@Nonnull final MultiwayTree<T> treeNode) {
		treeNode.setParentNode(this);
		if (this.childNodes.stream().noneMatch(existNode ->
				ObjectUtils.nullSafeEquals(treeNode.getNodeValue(), existNode.getNodeValue()))) {
			this.childNodes.add(treeNode);
		} else {
			this.childNodes.replaceAll(existNode -> {
				if (ObjectUtils.nullSafeEquals(treeNode.getNodeValue(), existNode.getNodeValue())) {
					return treeNode;
				}
				return existNode;
			});
		}
	}

	/**
	 * <h3 class="en-US">Recursive traversal</h3>
	 * <h3 class="zh-CN">递归遍历</h3>
	 *
	 * @return <span class="en-US">Node value queue</span>
	 * <span class="zh-CN">节点值队列</span>
	 */
	public Queue<T> recursion() {
		return this.recursion(RecursionType.Normal);
	}

	/**
	 * <h3 class="en-US">Recursive traversal</h3>
	 * <h3 class="zh-CN">递归遍历</h3>
	 *
	 * @param recursionType <span class="en-US">Recursive type enumeration value</span>
	 *                      <span class="zh-CN">递归类型枚举值</span>
	 * @return <span class="en-US">Node value queue</span>
	 * <span class="zh-CN">节点值队列</span>
	 */
	public Queue<T> recursion(final RecursionType recursionType) {
		Queue<T> queue = new LinkedList<>();
		switch (recursionType) {
			case Breadth:
				breadth(queue, this);
				break;
			case Depth:
				depth(queue, this);
				break;
			default:
				normal(queue, this);
				break;
		}
		return queue;
	}

	/**
	 * <h3 class="en-US">Recursive traversal</h3>
	 * <h3 class="zh-CN">递归遍历</h3>
	 *
	 * @param queue    <span class="en-US">Node value queue</span>
	 *                 <span class="zh-CN">节点值队列</span>
	 * @param treeNode <span class="en-US">Current traversal node</span>
	 *                 <span class="zh-CN">当前遍历节点</span>
	 */
	private static <T> void normal(@Nonnull final Queue<T> queue, @Nonnull final MultiwayTree<T> treeNode) {
		queue.offer(treeNode.getNodeValue());
		treeNode.getChildNodes().forEach(childNode -> normal(queue, childNode));
	}

	/**
	 * <h3 class="en-US">Breadth-first recursive traversal</h3>
	 * <h3 class="zh-CN">广度优先递归遍历</h3>
	 *
	 * @param queue    <span class="en-US">Node value queue</span>
	 *                 <span class="zh-CN">节点值队列</span>
	 * @param treeNode <span class="en-US">Current traversal node</span>
	 *                 <span class="zh-CN">当前遍历节点</span>
	 */
	private static <T> void breadth(@Nonnull final Queue<T> queue, @Nonnull final MultiwayTree<T> treeNode) {
		Deque<MultiwayTree<T>> nodeQueue = new LinkedList<>();
		nodeQueue.push(treeNode);
		while (!nodeQueue.isEmpty()) {
			MultiwayTree<T> node = nodeQueue.pop();
			queue.offer(node.getNodeValue());
			nodeQueue.addAll(node.getChildNodes());
		}
	}

	/**
	 * <h3 class="en-US">Depth-first recursive traversal</h3>
	 * <h3 class="zh-CN">深度优先递归遍历</h3>
	 *
	 * @param queue    <span class="en-US">Node value queue</span>
	 *                 <span class="zh-CN">节点值队列</span>
	 * @param treeNode <span class="en-US">Current traversal node</span>
	 *                 <span class="zh-CN">当前遍历节点</span>
	 */
	private static <T> void depth(@Nonnull final Queue<T> queue, @Nonnull final MultiwayTree<T> treeNode) {
		Deque<MultiwayTree<T>> nodeQueue = new LinkedList<>();
		nodeQueue.push(treeNode);
		while (!nodeQueue.isEmpty()) {
			MultiwayTree<T> node = nodeQueue.pop();
			queue.offer(node.getNodeValue());
			List<MultiwayTree<T>> childNodes = new ArrayList<>(node.getChildNodes());
			Collections.reverse(childNodes);
			nodeQueue.addAll(childNodes);
		}
	}
}
