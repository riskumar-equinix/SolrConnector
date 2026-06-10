package com.mysolrproject.constants;

public final class Constants {

    private Constants() {}

    /** Default Solr base URL (overridden by solr.url in application.properties). */
    public static final String SOLR_URL_DEFAULT = "http://localhost:8983/solr/oracle-core";

    /** Number of rows to fetch per page from Oracle (OFFSET/FETCH). Use for large result sets (e.g. 1M+). */
    public static final int FETCH_PAGE_SIZE = 500;

    public static final String SEARCHABLE_TASK_QUERY = "SELECT DISTINCT\n" +
            "ACT.ROW_ID,\n" +
            "PAM.X_ATTRIB_12 AS PAMGrp,\n" +
            "(PAM.X_JARVIS_CTG || ACT.Assess_10) AS JarvisCategory,\n" +
            "PAM.X_ATTRIB_1 AS PAMActionCode,\n" +
            "ACT.ASSESS_10 AS OpenStatus,\n" +
            "ACT.ACTIVITY_UID AS ActivityNumber,\n" +
            "ACT.TODO_CD AS ActivityType,\n" +
            "ACT.SUBTYPE_CD AS ActivitySubType,\n" +
            "I.NAME AS IBXName,\n" +
            "I.ROW_ID AS IBXRowId,\n" +
            "I.OU_TYPE_CD AS IBXType,\n" +
            "I.X_DCIM_ENABLE_FLG AS DCIMEnableFlag,\n" +
            "I.ACCNT_FLG AS AccountFlag,\n" +
            "I.X_OF_ATTRIB_03 AS MixedUseIBX,\n" +
            "I.REGION AS IBXRegion,\n" +
            "I.X_IBX_COUNTRY AS IBXCountry,\n" +
            "I.X_OCC_KB_FLAG AS OCCKBAllowedFlag,\n" +
            "I.LOC AS Location,\n" +
            "I.EVT_LOC_FLG AS IBXAvailabilityFlag,\n" +
            "I.AGENCY_NIBRS_IND AS IbxMarketType,\n" +
            "AA.COUNTRY AS Country,\n" +
            "A.NAME AS AccountName,\n" +
            "A.X_MASKED_ACCNT AS MaskedAccount,\n" +
            "A.OU_NUM AS CSN,\n" +
            "G.NAME AS GroupName,\n" +
            "G.ROW_ID AS ActGrpId,\n" +
            "G.X_NEW_ACT_UI_FLG AS NewActivityUI,\n" +
            "G.ROW_ID AS GrpRowId,\n" +
            "SU.ROW_ID AS SUserRowId,\n" +
            "SO.X_EXT_REF_NUM3 AS ExternalReferenceNumberThree,\n" +
            "SO.ORDER_NUM AS OrderNumber,\n" +
            "SO.ROW_ID AS SORowId,\n" +
            "SO.STATUS_CD AS OrderStatus,\n" +
            "SSO.ORDER_NUM AS SalesOrderNumber,\n" +
            "C.FST_NAME AS OrderConFirstName,\n" +
            "C.LAST_NAME AS OrderConLastName,\n" +
            "C.EMAIL_ADDR AS EmailAddress,\n" +
            "OI.X_OF_ATTRIB_12 AS EQXTechContactMobilePhone,\n" +
            "OI.X_TECH_CON_TZ_2 AS EQXTechMobilePhoneTimeZone,\n" +
            "OI.X_TECH_CON_TZ AS EQXTechWorkPhoneTimeZone,\n" +
            "OI.ROOT_ORDER_ITEM_ID AS RootOrderItemId,\n" +
            "OI.X_SERIAL_NUM AS SerialNumber,\n" +
            "OI.X_PROD_DESC AS ProductDesc,\n" +
            "OI.X_OM_ATTRIB_17 AS SegmentSide,\n" +
            "OI.X_CAB_UNQ_SPC_PRM_ID AS CabinetUniqueSpacePrimaryId,\n" +
            "MAUProd.NAME AS MAUProdElement,\n" +
            "MAUProd.PART_NUM AS MAUPartNumber,\n" +
            "SA.ASSET_NUM AS AssetNumber,\n" +
            "SAA.X_EQX_CAGE_RESTRICTION_FLG AS ASideFlag,\n" +
            "SAZ.X_EQX_CAGE_RESTRICTION_FLG AS ZSideFlag,\n" +
            "ActUSI.EQX_UNIQUE_SPACE_ID AS UniqueSpaceName,\n" +
            "USI.EQX_CABINET_NUMBER AS CabinetNumber,\n" +
            "USI.EQX_UNIQUE_SPACE_ID AS CABSPCUniqueSpaceId,\n" +
            "CUSI.EQX_UNIQUE_SPACE_ID AS EQXUniqueSpaceId,\n" +
            "USI.EQX_CAGE_UNQ_SPACE_ID_VAL AS CageUniqueSpaceIdValue,\n" +
            "CharValUSI.EQX_UNIQUE_SPACE_ID AS CabUniqSpaceIdValue,\n" +
            "SOI.x_tech_con_ucid AS EQXTechContactName,\n" +
            "SOI.X_OF_ATTRIB_09 AS EQXTechContactMobilePhonePrefToCall,\n" +
            "SOI.X_OF_ATTRIB_09 AS EQXTechContactWorkPhonePrefToCall,\n" +
            "OIX.ATTRIB_01 AS TechContactEmail,\n" +
            "OIX.ATTRIB_34 AS TechContactPhone,\n" +
            "CC.FST_NAME AS FirstName,\n" +
            "CC.LAST_NAME AS LastName,\n" +
            "KP.name AS ProductElement,\n" +
            "KP.PART_NUM AS ActDetailsKBType,\n" +
            "P.NAME AS ProductNamePOF,\n" +
            "P.PART_NUM AS KBTypeConstrain,\n" +
            "ActProd.NAME AS ActProdName,\n" +
            "ActProd.PART_NUM AS ActProdPartNum,\n" +
            "CSN.EQX_SYS_NAME AS ActSystemName,\n" +
            "CSN.ROW_ID AS CSNRowId,\n" +
            "CSN.EQX_CAGE_UNQ_SPACE_ID AS CageUniqueSpaceId,\n" +
            "CSNCC.EQX_SYS_NAME AS OrderSystemName,\n" +
            "(\n" +
            "  SELECT SIBLINGOI.ROW_ID\n" +
            "  FROM SIEBEL.S_ORDER_ITEM SIBLINGOI\n" +
            "  JOIN SIEBEL.S_PROD_INT WorkVistProduct\n" +
            "    ON SIBLINGOI.PROD_ID = WorkVistProduct.ROW_ID\n" +
            "  WHERE P.NAME = 'Scheduled Services'\n" +
            "    AND WorkVistProduct.PART_NUM = 'PS00012.ELEM'\n" +
            "    AND SIBLINGOI.ROOT_ORDER_ITEM_ID = OI.ROOT_ORDER_ITEM_ID\n" +
            ") AS WorkVisitOrderItemId,\n" +
            "(\n" +
            "  SELECT COUNT(\n" +
            "    CASE\n" +
            "      WHEN LOV.ACTIVE_FLG = 'Y'\n" +
            "       AND LOV.VAL = 'SH'\n" +
            "       AND LOV.TYPE = 'EQX_REVAMPUI_PART_NUM'\n" +
            "       AND LOV.NAME = ActProd.PART_NUM\n" +
            "      THEN 1\n" +
            "    END\n" +
            "  )\n" +
            "  FROM SIEBEL.S_LST_OF_VAL LOV\n" +
            ") AS RevampUI_SH,\n" +
            "(\n" +
            "  SELECT COUNT(\n" +
            "    CASE\n" +
            "      WHEN LOV.ACTIVE_FLG = 'Y'\n" +
            "       AND LOV.VAL != 'SH'\n" +
            "       AND LOV.TYPE = 'EQX_REV_ST'\n" +
            "       AND LOV.SUB_TYPE = 'YourActivitySubType'\n" +
            "      THEN 1\n" +
            "    END\n" +
            "  )\n" +
            "  FROM SIEBEL.S_LST_OF_VAL LOV\n" +
            ") AS LOVFlag_SH\n" +
            "FROM SIEBEL.S_EVT_ACT_X ACTX\n" +
            "JOIN SIEBEL.S_EVT_ACT ACT ON ACTX.PAR_ROW_ID = ACT.ROW_ID\n" +
            "LEFT JOIN SIEBEL.S_ORG_EXT I ON I.ROW_ID = ACT.X_IBX_ID\n" +
            "LEFT JOIN SIEBEL.S_ADDR_PER AA ON I.PR_ADDR_ID = AA.ROW_ID\n" +
            "LEFT JOIN SIEBEL.S_ORG_EXT A ON A.ROW_ID = ACT.TARGET_OU_ID\n" +
            "LEFT JOIN SIEBEL.S_ORG_EXT G ON G.ROW_ID = ACT.X_GROUP_ID\n" +
            "LEFT JOIN SIEBEL.S_ORDER SO ON SO.ROW_ID = ACT.ORDER_ID\n" +
            "LEFT JOIN SIEBEL.S_ORDER SSO ON SSO.ROW_ID = ACT.X_SALES_ORDER_ID\n" +
            "LEFT JOIN SIEBEL.S_CONTACT C ON C.PAR_ROW_ID = SSO.CONTACT_ID\n" +
            "LEFT JOIN SIEBEL.S_USER SU ON SU.LOGIN = ACT.X_JARVIS_LOGIN\n" +
            "LEFT JOIN SIEBEL.S_ORDER_ITEM OI ON OI.ROW_ID = ACT.ORDER_ITEM_ID\n" +
            "LEFT JOIN SIEBEL.CX_UNI_SYS_NAME CSNCC\n" +
            "  ON CSNCC.EQX_UCM_ID = OI.X_OF_ATTRIB_07\n" +
            "  AND CSNCC.EQX_CAGE_UNQ_SPACE_ID = OI.X_UNIQ_SPC_PRM_ID\n" +
            "LEFT JOIN SIEBEL.S_PROD_INT MAUProd ON MAUProd.ROW_ID = OI.PROD_ID\n" +
            "LEFT JOIN SIEBEL.S_ASSET SA ON SA.INTEGRATION_ID = OI.ASSET_INTEG_ID\n" +
            "LEFT JOIN SIEBEL.S_ASSET SAA ON SAA.ROW_ID = ACT.X_ASIDE_ASSET_ID\n" +
            "LEFT JOIN SIEBEL.S_ASSET SAZ ON SAZ.ROW_ID = ACT.X_ZSIDE_ASSET_ID\n" +
            "LEFT JOIN SIEBEL.CX_EQX_UNQ_SPC ActUSI ON ActUSI.EQX_UNQ_SPC_ID = ACT.X_ACT_UNI_SPC_ID\n" +
            "LEFT JOIN SIEBEL.CX_EQX_UNQ_SPC CharValUSI\n" +
            "  ON CharValUSI.EQX_UNQ_SPC_ID = ACT.X_ACT_UNI_SPC_ID\n" +
            "LEFT JOIN SIEBEL.CX_EQX_UNQ_SPC USI ON USI.EQX_UNQ_SPC_ID = OI.X_CAB_UNQ_SPC_PRM_ID\n" +
            "LEFT JOIN SIEBEL.CX_EQX_UNQ_SPC CUSI ON CUSI.EQX_UNQ_SPC_ID = OI.X_UNIQ_SPC_PRM_ID\n" +
            "LEFT JOIN SIEBEL.S_ORDER_ITEM SOI ON SOI.ROW_ID = ACT.X_SALES_ORDER_ITEM_ID\n" +
            "LEFT JOIN SIEBEL.S_ORDER_ITEM_X OIX ON OIX.ROW_ID = ACT.X_SALES_ORDER_ITEM_ID\n" +
            "LEFT JOIN SIEBEL.S_CONTACT CC ON CC.PAR_ROW_ID = ACT.OWNER_PER_ID\n" +
            "LEFT JOIN SIEBEL.S_PROD_INT KP ON KP.ROW_ID = ACT.PR_PRDINT_ID\n" +
            "LEFT JOIN SIEBEL.S_PROD_INT P ON P.ROW_ID = ACTX.X_ROOT_PROD_ID\n" +
            "LEFT JOIN SIEBEL.S_PROD_INT ActProd ON ActProd.ROW_ID = ACT.PR_PRDINT_ID\n" +
            "LEFT JOIN SIEBEL.CX_UNI_SYS_NAME CSN\n" +
            "  ON CSN.EQX_UCM_ID = ACT.X_OI_UCM_ID\n" +
            "  AND CSN.EQX_CAGE_UNQ_SPACE_ID = ACT.X_ACT_UNI_SPC_ID\n" +
            "LEFT JOIN SIEBEL.CX_OF_MASTER PAM\n" +
            "  ON PAM.X_IDENTIFIER = 'Product Activity'\n" +
            "  AND PAM.X_ATTRIB_2 = ACT.TODO_CD\n" +
            "  AND PAM.X_ATTRIB_3 = ACT.SUBTYPE_CD\n" +
            "  AND (\n" +
            "    (\n" +
            "      (PAM.X_ATTRIB_1 = 'Add'\n" +
            "        OR (PAM.X_ATTRIB_1 = 'Update' AND PAM.X_ATTRIB_3 = 'Power Meter')\n" +
            "      )\n" +
            "      AND PAM.X_ATTRIB_2 IN ('Install','BCTR Test','BCTR Recovery','Smart Hands','Change')\n" +
            "      AND PAM.X_JARVIS_CTG IS NOT NULL\n" +
            "    )\n" +
            "    OR (\n" +
            "      (PAM.X_ATTRIB_1 = 'Add' AND PAM.X_ATTRIB_3 IN ('Expansion','Reduction'))\n" +
            "      OR (PAM.X_ATTRIB_1 = 'Update' AND PAM.X_ATTRIB_3 IN ('Merge','Split'))\n" +
            "    )\n" +
            "    AND PAM.X_ATTRIB_2 = 'Cage Reconfiguration'\n" +
            "  )\n" +
            "WHERE ACT.CREATED >= TO_DATE('01-01-2025','DD-MM-YYYY')\n" +
            "  AND ACT.TODO_CD = 'Install'\n" +
            "ORDER BY ACT.ROW_ID";
}
