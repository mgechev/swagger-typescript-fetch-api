package com.rhyme.codegen;

import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.languages.AbstractTypeScriptClientCodegen;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.FileProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class TypeScriptFetchApiGenerator extends AbstractTypeScriptClientCodegen {

    public TypeScriptFetchApiGenerator() {
        super();

        // clear import mapping (from default generator) as TS does not use it
        // at the moment
        importMapping.clear();

        outputFolder = "generated-code/typescript-fetch-api";
        embeddedTemplateDir = templateDir = "typescript-fetch-api";
    }
    
    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, ModelImpl swaggerModel) {
        codegenModel.additionalPropertiesType = getSwaggerType(swaggerModel.getAdditionalProperties());
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }
    
    @Override
    public String getTypeDeclaration(Property p) {
        Property inner;
        if(p instanceof ArrayProperty) {
            ArrayProperty mp1 = (ArrayProperty)p;
            inner = mp1.getItems();
            return this.getSwaggerType(p) + "<" + this.getTypeDeclaration(inner) + ">";
        } else if(p instanceof MapProperty) {
            MapProperty mp = (MapProperty)p;
            inner = mp.getAdditionalProperties();
            return "{ [key: string]: " + this.getTypeDeclaration(inner) + "; }";
        } else if(p instanceof FileProperty || p instanceof ObjectProperty) {
            return "any";
        } else {
            return super.getTypeDeclaration(p);
        }
    }
    
    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        if(languageSpecificPrimitives.contains(swaggerType)) {
            return swaggerType;
        }
        return addModelPrefix(swaggerType);
    }
    
    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        parameter.dataType = addModelPrefix(parameter.dataType);
    }
    

    private String addModelPrefix(String swaggerType) {
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
        } else {
            type = swaggerType;
        }
        return type;
    }


    private boolean startsWithLanguageSpecificPrimitiv(String type) {
        for (String langPrimitive:languageSpecificPrimitives) {
            if (type.startsWith(langPrimitive))  {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.add(new SupportingFile("api.mustache", "", "api.ts"));
    }

    @Override
    public String getName() {
        return "typescript-fetch-api";
    }

    @Override
    public String getHelp() {
        return "Generates a TypeScript client library using Fetch API (beta).";
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // process enum in models
        List<Object> models = (List<Object>) postProcessModelsEnum(objs).get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");
            cm.imports = new TreeSet(cm.imports);
            for (CodegenProperty var : cm.vars) {
                // name enum with model name, e.g. StatuEnum => PetStatusEnum
                if (Boolean.TRUE.equals(var.isEnum)) {
                    var.datatypeWithEnum = var.datatypeWithEnum.replace(var.enumName, cm.classname + var.enumName);
                    var.enumName = cm.classname + var.enumName;
                }
            }
        }

        return objs;
    }

}
