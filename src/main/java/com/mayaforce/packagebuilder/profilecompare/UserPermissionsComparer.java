/*
 * Copyright 2024 EricHaycraft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mayaforce.packagebuilder.profilecompare;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UserPermissionsComparer implements TagComparer {

    // this Comparer will look at a userPermissions node and compare it to another one like it
    // will return true if name and enabled are identical, else false
    //  <userPermissions>
    //        <enabled>true</enabled>
    //        <name>ActivateOrder</name>
    //    </userPermissions>
    private static final String MYNODENAME = "userPermissions";
    private static final String ENABLED = "enabled";
    private static final String NAME = "name";

    @Override
    public boolean isIdentical(Node source, Node target) {

        // first check of both Nodes we're dealing with are userPermissions - if not return false
        if (!source.getNodeName().equals(MYNODENAME) || !target.getNodeName().equals(MYNODENAME)) {
            // something is not right, we need to compare two userPermissions nodes
            return false;
        }

        Node enabledSource = null;
        Node enabledTarget = null;
        Node nameSource = null;
        Node nameTarget = null;

        NodeList children = source.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node currentNode = children.item(i);
            if (currentNode.getNodeName().equals(ENABLED)) {
                enabledSource = currentNode;
            } else if (currentNode.getNodeName().equals(NAME)) {
                nameSource = currentNode;
            }
        }

        children = target.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node currentNode = children.item(i);
            if (currentNode.getNodeName().equals(ENABLED)) {
                enabledTarget = currentNode;
            } else if (currentNode.getNodeName().equals(NAME)) {
                nameTarget = currentNode;
            }
        }

        if (enabledSource == null
                || enabledTarget == null
                || nameSource == null
                || nameTarget == null) {
            // we apparently haven't found what we're looking for

            return false;
        }

        return enabledSource.getTextContent().equals(enabledTarget.getTextContent())
                && nameSource.getTextContent().equals(nameTarget.getTextContent());
    }

}
