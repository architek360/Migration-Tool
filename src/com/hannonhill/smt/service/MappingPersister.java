/*
 * Created on Dec 16, 2009 by Artur Tomusiak
 * 
 * Copyright(c) 2000-2009 Hannon Hill Corporation. All rights reserved.
 */
package com.hannonhill.smt.service;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.hannonhill.smt.AssetType;
import com.hannonhill.smt.ContentTypeInformation;
import com.hannonhill.smt.DataDefinitionField;
import com.hannonhill.smt.ExternalRootLevelFolderAssignment;
import com.hannonhill.smt.Field;
import com.hannonhill.smt.MetadataSetField;
import com.hannonhill.smt.ProjectInformation;
import com.hannonhill.smt.util.XmlUtil;

/**
 * A service that takes care of persisting the content type and field mappings on the server's file system and
 * retrieving back that information.
 * 
 * @author Artur Tomusiak
 * @since 1.0
 */
public class MappingPersister
{
    private static final String PROJECT_INFORMATION_TAG = "projectInformation";
    private static final String ASSET_TYPES_TAG = "assetTypes";
    private static final String ASSET_TYPE_TAG = "assetType";
    private static final String NAME_TAG = "name";
    private static final String MAPPED_CONTENT_TYPE_PATH_TAG = "mappedContentTypePath";
    private static final String METADATA_FIELD_MAPPINGS_TAG = "metadataFieldMappings";
    private static final String VAR_FIELD_MAPPINGS_TAG = "varFieldMappings";
    private static final String CONTENT_FIELD_MAPPINGS_TAG = "contentFieldMappings";
    private static final String FIELD_MAPPING_TAG = "fieldMapping";
    private static final String FIELD_NAME_TAG = "fieldName";
    private static final String CASCADE_METADATA_FIELD_TAG = "cascadeMetadataField";
    private static final String CASCADE_DATA_DEFINITION_FIELD_TAG = "cascadeDataDefinitionFieldtag";
    private static final String STATIC_VALUE_MAPPINGS_TAG = "staticValueMappings";
    private static final String STATIC_VALUE_MAPPING_TAG = "staticValueMapping";
    private static final String STATIC_VALUE_TAG = "staticValue";
    private static final String ROOT_LEVEL_FOLDERS_TAG = "rootLevelFolders";
    private static final String ROOT_LEVEL_FOLDER_TAG = "rootLevelFolder";
    private static final String FOLDER_TAG = "folder";
    private static final String CROSS_SITE_TAG = "crossSite";
    private static final String EXTERNAL_LINK_TAG = "externalLink";

    /**
     * Saves the mappings from the projectInformation into the server's file system
     * 
     * @param projectInformation
     * @throws Exception
     */
    public static void persistMappings(ProjectInformation projectInformation) throws Exception
    {
        StringBuilder content = new StringBuilder();
        content.append("<" + PROJECT_INFORMATION_TAG + ">");

        content.append("<" + ASSET_TYPES_TAG + ">");
        for (AssetType assetType : projectInformation.getAssetTypes().values())
            persistAssetType(content, projectInformation, assetType);
        content.append("</" + ASSET_TYPES_TAG + ">");

        content.append("<" + ROOT_LEVEL_FOLDERS_TAG + ">");
        for (String folder : projectInformation.getExternalRootLevelFolderAssignemnts().keySet())
            persistRootLevelFolder(content, projectInformation.getExternalRootLevelFolderAssignemnts().get(folder));
        content.append("</" + ROOT_LEVEL_FOLDERS_TAG + ">");

        content.append("</" + PROJECT_INFORMATION_TAG + ">");

        String xmlFilePath = projectInformation.getXmlDirectory() + ".xml";
        FileSystem.saveFile(xmlFilePath, content.toString());
    }

    /**
     * Loads the mappings from the file system and assigns them to the projectInformation object. If there is
     * a problem with loading the saved mappings,
     * nothing significant will happen - no mappings will be loaded and the stack trace will be in the output.
     * 
     * @param projectInformation
     */
    public static void loadMappings(ProjectInformation projectInformation)
    {
        try
        {
            String xmlFilePath = projectInformation.getXmlDirectory() + ".xml";
            File file = new File(xmlFilePath);

            // If the file doesn't exist, don't do anything
            if (!file.exists())
                return;

            Node rootNode = XmlUtil.convertXmlToNodeStructure(new InputSource(new FileInputStream(file)));

            // clear existing asset types
            projectInformation.getContentTypeMap().clear();

            for (int i = 0; i < rootNode.getChildNodes().getLength(); i++)
            {
                Node node = rootNode.getChildNodes().item(i);
                if (node.getNodeName().equals(ASSET_TYPES_TAG))
                    loadAssetTypes(projectInformation, node);
                if (node.getNodeName().equals(ROOT_LEVEL_FOLDERS_TAG))
                    loadRootLevelFolders(projectInformation, node);
            }
        }
        catch (Exception e)
        {
            // If problem occured, don't do anything. Just show the stack trace.
            e.printStackTrace();
        }
    }

    /**
     * Loads the asset mappings which consist of field mappings as well
     * 
     * @param projectInformation
     * @param assetTypesNode
     */
    private static void loadAssetTypes(ProjectInformation projectInformation, Node assetTypesNode)
    {
        for (int i = 0; i < assetTypesNode.getChildNodes().getLength(); i++)
        {
            Node node = assetTypesNode.getChildNodes().item(i);
            if (node.getNodeName().equals(ASSET_TYPE_TAG))
                loadAssetType(projectInformation, node);
        }
    }

    /**
     * Loads the root level folder mappings
     * 
     * @param projectInformation
     * @param rootLevelFolderNode
     */
    private static void loadRootLevelFolders(ProjectInformation projectInformation, Node rootLevelFolderNode)
    {
        for (int i = 0; i < rootLevelFolderNode.getChildNodes().getLength(); i++)
        {
            Node node = rootLevelFolderNode.getChildNodes().item(i);
            if (node.getNodeName().equals(ROOT_LEVEL_FOLDER_TAG))
                loadRootLevelFolder(projectInformation, node);
        }
    }

    /**
     * Creates an assignment exactly as described in the XML
     * 
     * @param projectInformation
     * @param rootLevelFolderNode
     */
    private static void loadRootLevelFolder(ProjectInformation projectInformation, Node rootLevelFolderNode)
    {
        String folder = null;
        String crossSiteAssignment = null;
        String externalLinkAssignment = null;

        for (int i = 0; i < rootLevelFolderNode.getChildNodes().getLength(); i++)
        {
            Node node = rootLevelFolderNode.getChildNodes().item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals(FOLDER_TAG))
                folder = node.getTextContent();
            else if (nodeName.equals(CROSS_SITE_TAG))
                crossSiteAssignment = node.getTextContent();
            else if (nodeName.equals(EXTERNAL_LINK_TAG))
                externalLinkAssignment = node.getTextContent();
        }
        ExternalRootLevelFolderAssignment assignment = new ExternalRootLevelFolderAssignment(folder, crossSiteAssignment, externalLinkAssignment);
        projectInformation.getExternalRootLevelFolderAssignemnts().put(folder, assignment);
    }

    /**
     * Finds the asset type with the name in the &lt;name&gt; child tag and assigns its available properties.
     * If the asset type with given name could not be found, it gets ignored.
     * 
     * @param projectInformation
     * @param assetTypeNode
     */
    private static void loadAssetType(ProjectInformation projectInformation, Node assetTypeNode)
    {
        // find the asset type name and the mapped content type path
        String assetTypeName = null;
        String mappedContentTypePath = null;
        Node metadataFieldMappingsNode = null;
        Node varFieldMappingsNode = null;
        Node contentFieldMappingsNode = null;
        Node staticValueMappingsNode = null;
        for (int i = 0; i < assetTypeNode.getChildNodes().getLength(); i++)
        {
            Node node = assetTypeNode.getChildNodes().item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals(NAME_TAG))
                assetTypeName = node.getTextContent();
            else if (nodeName.equals(MAPPED_CONTENT_TYPE_PATH_TAG))
                mappedContentTypePath = node.getTextContent();
            else if (nodeName.equals(METADATA_FIELD_MAPPINGS_TAG))
                metadataFieldMappingsNode = node;
            else if (nodeName.equals(VAR_FIELD_MAPPINGS_TAG))
                varFieldMappingsNode = node;
            else if (nodeName.equals(CONTENT_FIELD_MAPPINGS_TAG))
                contentFieldMappingsNode = node;
            else if (nodeName.equals(STATIC_VALUE_MAPPINGS_TAG))
                staticValueMappingsNode = node;
        }

        // if they couldn't be found, then skip this one
        if (assetTypeName == null || mappedContentTypePath == null)
            return;

        // Check if the asset type and content type like that still exists (it's possible that the contents of
        // the serena xml files changed
        // and it doesn't exist anymore or Content Type in Cascade got deleted in meantime) and if it doesn't
        // exist, then just ignore this mapping.
        AssetType assetType = projectInformation.getAssetTypes().get(assetTypeName);
        ContentTypeInformation contentType = projectInformation.getContentTypes().get(mappedContentTypePath);

        // If either asset type or content type could not be found, it means that this mapping is invalid, so
        // just ignore it
        if (assetType == null || contentType == null)
            return;

        projectInformation.getContentTypeMap().put(assetTypeName, mappedContentTypePath);

        // clear existing mappings
        assetType.getMetadataFieldMapping().clear();
        assetType.getVarFieldMapping().clear();
        assetType.getContentFieldMapping().clear();
        assetType.getStaticValueMapping().clear();

        // load new mappings
        loadFieldMappings(metadataFieldMappingsNode, assetType.getMetadataFieldMapping(), contentType);
        loadFieldMappings(varFieldMappingsNode, assetType.getVarFieldMapping(), contentType);
        loadFieldMappings(contentFieldMappingsNode, assetType.getContentFieldMapping(), contentType);
        loadStaticValueMappings(staticValueMappingsNode, assetType.getStaticValueMapping(), contentType);
    }

    /**
     * Assigns the static value mappings from each &lt;fieldMapping&gt; child to the mappings. Finds the field
     * object by its identifier in the content type.
     * If the field object could not be found, it gets ignored.
     * 
     * @param mappingsNode
     * @param mappings
     * @param contentType
     */
    private static void loadFieldMappings(Node mappingsNode, Map<String, Field> mappings, ContentTypeInformation contentType)
    {
        for (int i = 0; i < mappingsNode.getChildNodes().getLength(); i++)
        {
            Node node = mappingsNode.getChildNodes().item(i);
            if (node.getNodeName().equals(FIELD_MAPPING_TAG))
                loadFieldMapping(node, mappings, contentType);
        }
    }

    /**
     * Assigns the field mappings from given node to the mappings. Finds the field object by its identifier in
     * the content type.
     * If the field object could not be found, it gets ignored.
     * 
     * @param mappingNode
     * @param mappings
     * @param contentType
     */
    private static void loadFieldMapping(Node mappingNode, Map<String, Field> mappings, ContentTypeInformation contentType)
    {
        String fieldName = null;
        String cascadeMetadataField = null;
        String cascadeDataDefinitionField = null;
        for (int i = 0; i < mappingNode.getChildNodes().getLength(); i++)
        {
            Node node = mappingNode.getChildNodes().item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals(FIELD_NAME_TAG))
                fieldName = node.getTextContent();
            else if (nodeName.equals(CASCADE_METADATA_FIELD_TAG))
                cascadeMetadataField = node.getTextContent();
            else if (nodeName.equals(CASCADE_DATA_DEFINITION_FIELD_TAG))
                cascadeDataDefinitionField = node.getTextContent();
        }

        // Quit if the field name wasn't provided or neither metadata nor data definition field was provided
        if ((fieldName == null) || (cascadeMetadataField == null && cascadeDataDefinitionField == null))
            return;

        // Quit if the field with given identifier does not exist anymore - it's possible that the metadata
        // set or data definition was modified
        Field field = getField(contentType, cascadeMetadataField, cascadeDataDefinitionField);
        if (field == null)
            return;

        mappings.put(fieldName, field);
    }

    /**
     * Assigns the static value mappings from each &lt;staticValueMapping&gt; child to the mappings. Finds the
     * field object by its identifier in the content type.
     * If the field object could not be found, it gets ignored.
     * 
     * @param mappingsNode
     * @param mappings
     * @param contentType
     */
    private static void loadStaticValueMappings(Node mappingsNode, Map<Field, String> mappings, ContentTypeInformation contentType)
    {
        for (int i = 0; i < mappingsNode.getChildNodes().getLength(); i++)
        {
            Node node = mappingsNode.getChildNodes().item(i);
            if (node.getNodeName().equals(STATIC_VALUE_MAPPING_TAG))
                loadStaticValueMapping(node, mappings, contentType);
        }
    }

    /**
     * Assigns the static value mappings from given node to the mappings. Finds the field object by its
     * identifier in the content type.
     * If the field object could not be found, it gets ignored.
     * 
     * @param mappingNode
     * @param mappings
     * @param contentType
     */
    private static void loadStaticValueMapping(Node mappingNode, Map<Field, String> mappings, ContentTypeInformation contentType)
    {
        String cascadeMetadataField = null;
        String cascadeDataDefinitionField = null;
        String staticValue = null;
        for (int i = 0; i < mappingNode.getChildNodes().getLength(); i++)
        {
            Node node = mappingNode.getChildNodes().item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals(CASCADE_METADATA_FIELD_TAG))
                cascadeMetadataField = node.getTextContent();
            else if (nodeName.equals(CASCADE_DATA_DEFINITION_FIELD_TAG))
                cascadeDataDefinitionField = node.getTextContent();
            else if (nodeName.equals(STATIC_VALUE_TAG))
                staticValue = node.getTextContent();
        }

        // Quit if the neither metadata nor data definition field was provided or static value wasn't provided
        if ((cascadeMetadataField == null && cascadeDataDefinitionField == null) || staticValue == null)
            return;

        // Quit if the field with given identifier does not exist anymore - it's possible that the metadata
        // set or data definition was modified
        Field field = getField(contentType, cascadeMetadataField, cascadeDataDefinitionField);
        if (field == null)
            return;

        mappings.put(field, staticValue);
    }

    /**
     * If cascadeMetadataField is not null, returns a metadata field from the content type that matches the
     * cascadeMetadataField identifier.
     * If not, returns a data definition field from the content type that matches the
     * cascadeDataDefinitionField identifier.
     * 
     * @param contentType
     * @param cascadeMetadataField
     * @param cascadeDataDefinitionField
     * @return
     */
    private static Field getField(ContentTypeInformation contentType, String cascadeMetadataField, String cascadeDataDefinitionField)
    {
        return cascadeMetadataField != null ? contentType.getMetadataFields().get(cascadeMetadataField) : contentType.getDataDefinitionFields().get(
                cascadeDataDefinitionField);
    }

    /**
     * Adds the &lt;assetType&gt; tag to the content with all the information that needs to be stored about
     * that asset type
     * 
     * @param content
     * @param projectInformation
     * @param assetType
     */
    private static void persistAssetType(StringBuilder content, ProjectInformation projectInformation, AssetType assetType)
    {
        content.append("<" + ASSET_TYPE_TAG + ">");
        content.append("<" + NAME_TAG + ">" + assetType.getName() + "</" + NAME_TAG + ">");

        String contentTypePath = projectInformation.getContentTypeMap().get(assetType.getName());
        if (contentTypePath != null)
            content.append("<" + MAPPED_CONTENT_TYPE_PATH_TAG + ">" + contentTypePath + "</" + MAPPED_CONTENT_TYPE_PATH_TAG + ">");

        content.append("<" + METADATA_FIELD_MAPPINGS_TAG + ">");
        for (String metadataFieldName : assetType.getMetadataFieldMapping().keySet())
            persistFieldMapping(content, assetType.getMetadataFieldMapping(), metadataFieldName);
        content.append("</" + METADATA_FIELD_MAPPINGS_TAG + ">");

        content.append("<" + VAR_FIELD_MAPPINGS_TAG + ">");
        for (String varFieldName : assetType.getVarFieldMapping().keySet())
            persistFieldMapping(content, assetType.getVarFieldMapping(), varFieldName);
        content.append("</" + VAR_FIELD_MAPPINGS_TAG + ">");

        content.append("<" + CONTENT_FIELD_MAPPINGS_TAG + ">");
        for (String contentFieldName : assetType.getContentFieldMapping().keySet())
            persistFieldMapping(content, assetType.getContentFieldMapping(), contentFieldName);
        content.append("</" + CONTENT_FIELD_MAPPINGS_TAG + ">");

        content.append("<" + STATIC_VALUE_MAPPINGS_TAG + ">");
        for (Field field : assetType.getStaticValueMapping().keySet())
            persistStaticValueMapping(content, assetType.getStaticValueMapping(), field);
        content.append("</" + STATIC_VALUE_MAPPINGS_TAG + ">");

        content.append("</" + ASSET_TYPE_TAG + ">");
    }

    /**
     * Adds the &lt;rootLevelFolder&gt; tag to the content with the root level folder assignment information
     * that needs to be stored
     * 
     * @param content
     * @param assignment
     */
    private static void persistRootLevelFolder(StringBuilder content, ExternalRootLevelFolderAssignment assignment)
    {
        content.append("<" + ROOT_LEVEL_FOLDER_TAG + ">");
        content.append("<" + FOLDER_TAG + ">");
        content.append(assignment.getFolder());
        content.append("</" + FOLDER_TAG + ">");

        if (assignment.getCrossSiteAssignment() != null)
        {
            content.append("<" + CROSS_SITE_TAG + ">");
            content.append(assignment.getCrossSiteAssignment());
            content.append("</" + CROSS_SITE_TAG + ">");
        }

        if (assignment.getExternalLinkAssignment() != null)
        {
            content.append("<" + EXTERNAL_LINK_TAG + ">");
            content.append(assignment.getExternalLinkAssignment());
            content.append("</" + EXTERNAL_LINK_TAG + ">");
        }

        content.append("</" + ROOT_LEVEL_FOLDER_TAG + ">");
    }

    /**
     * Adds the &lt;fieldMapping&gt; tag to the content with information about the xml field name mapping to a
     * Cascade field
     * 
     * @param content
     * @param mapping
     * @param fieldName
     */
    private static void persistFieldMapping(StringBuilder content, Map<String, Field> mapping, String fieldName)
    {
        content.append("<" + FIELD_MAPPING_TAG + ">");
        content.append("<" + FIELD_NAME_TAG + ">" + fieldName + "</" + FIELD_NAME_TAG + ">");
        persistCascadeField(content, mapping.get(fieldName));
        content.append("</" + FIELD_MAPPING_TAG + ">");
    }

    /**
     * Adds the &lt;staticValueMapping*gt; tag to the content with information about Cascade field mapping to
     * a static value
     * 
     * @param content
     * @param mapping
     * @param field
     */
    private static void persistStaticValueMapping(StringBuilder content, Map<Field, String> mapping, Field field)
    {
        content.append("<" + STATIC_VALUE_MAPPING_TAG + ">");
        persistCascadeField(content, field);
        content.append("<" + STATIC_VALUE_TAG + ">" + mapping.get(field).replaceAll("&", "&amp;").replaceAll("<", "&lt;") + "</" + STATIC_VALUE_TAG
                + ">");
        content.append("</" + STATIC_VALUE_MAPPING_TAG + ">");
    }

    /**
     * Adds the &lt;cascadeMetadataField&gt; tag or &lt;cascadeDataDefinitionField&gt; tag depending on the
     * field type to the content
     * with the identifier of the field
     * 
     * @param content
     * @param cascadeField
     */
    private static void persistCascadeField(StringBuilder content, Field cascadeField)
    {
        if (cascadeField instanceof MetadataSetField)
            content.append("<" + CASCADE_METADATA_FIELD_TAG + ">" + cascadeField.getIdentifier() + "</" + CASCADE_METADATA_FIELD_TAG + ">");
        else if (cascadeField instanceof DataDefinitionField)
            content.append("<" + CASCADE_DATA_DEFINITION_FIELD_TAG + ">" + cascadeField.getIdentifier() + "</" + CASCADE_DATA_DEFINITION_FIELD_TAG
                    + ">");
    }
}
