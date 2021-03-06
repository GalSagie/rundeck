/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.plugin.example;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;
import com.dtolabs.rundeck.core.execution.dispatch.INodeEntryComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankTieredOrchestator implements Orchestrator {
    public static final Logger logger = LoggerFactory.getLogger(RankTieredOrchestator.class);

    private final List<INodeEntry> list;


    private INodeEntry previousNode;
    private final String rankAttribute;
    private Comparator<INodeEntry> comparator;
    private int count;

    public RankTieredOrchestator(StepExecutionContext context, Collection<INodeEntry> nodes) {
        this.rankAttribute = context.getNodeRankAttribute() != null ? context.getNodeRankAttribute() : "nodename";
        comparator = new NodeAttributeComparator(rankAttribute);
        this.list = INodeEntryComparator.rankOrderedNodes(
                nodes,
                context.getNodeRankAttribute(),
                context.isNodeRankOrderAscending()
        );

        if (context != null && context.getExecutionListener() != null) {
            context.getExecutionListener().log(
                    3,
                    "RankTieredOrchestator rank by " +
                    rankAttribute +
                    ", " +
                    (context.isNodeRankOrderAscending() ? "ascending" : "descending")
            );
        }
        this.count = 0;

    }


    @Override
    public INodeEntry nextNode() {
        if (list.size() < 1) {
            return null;
        }
        INodeEntry node = list.get(0);
        if (count == 0 || previousNode == null || comparator.compare(previousNode, node) == 0) {
            previousNode = node;
            count++;
            return list.remove(0);
        } else {
            return null;
        }
    }

    @Override
    public void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
        count--;
    }

    public boolean isComplete() {
        return list.size() == 0 && count == 0;
    }

    private static class NodeAttributeComparator implements Comparator<INodeEntry> {
        private String rankAttribute;

        public NodeAttributeComparator(final String rankAttribute) {
            this.rankAttribute = rankAttribute;
        }

        @Override
        public int compare(final INodeEntry o1, final INodeEntry o2) {
            String a1 = null != o1.getAttributes() ? o1.getAttributes().get(rankAttribute) : null;
            String a2 = null != o2.getAttributes() ? o2.getAttributes().get(rankAttribute) : null;
            if (a1 != null && a2 != null) {
                return a1.compareTo(a2);
            }
            if (a1 == null && a2 == null) {
                return 0;
            }
            return a1 != null ? -1 : 1;
        }
    }
}
