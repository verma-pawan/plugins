/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ofbiz.ebaystore;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.calendar.RecurrenceInfoException;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.job.JobManager;

import com.ebay.sdk.ApiAccount;
import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiCredential;
import com.ebay.sdk.ApiLogging;
import com.ebay.sdk.call.AddItemCall;
import com.ebay.soap.eBLBaseComponents.AddItemRequestType;
import com.ebay.soap.eBLBaseComponents.AddItemResponseType;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.BuyerPaymentMethodCodeType;
import com.ebay.soap.eBLBaseComponents.CategoryType;
import com.ebay.soap.eBLBaseComponents.CountryCodeType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.GeteBayDetailsResponseType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ListingDesignerType;
import com.ebay.soap.eBLBaseComponents.ListingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import com.ebay.soap.eBLBaseComponents.ReturnPolicyType;
import com.ebay.soap.eBLBaseComponents.ShippingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceOptionsType;
import com.ebay.soap.eBLBaseComponents.ShippingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SiteCodeType;
import com.ebay.soap.eBLBaseComponents.ShippingLocationDetailsType;
import com.ebay.soap.eBLBaseComponents.VATDetailsType;

import org.ofbiz.ebay.EbayHelper;

import sun.net.www.content.text.Generic;

public class EbayStoreHelper {
    private static final String configFileName = "ebayStore.properties";
    private static final String module = EbayStoreHelper.class.getName();
    public static final String resource = "EbayStoreUiLabels";

    public static ApiContext getApiContext(String productStoreId,Locale locale, Delegator delegator) {
       Map<String, Object> context = FastMap.newInstance();
       context.put("locale", locale);
       context.put("productStoreId", productStoreId);
       Map<String, Object> config = EbayHelper.buildEbayConfig(context, delegator);
       ApiCredential apiCredential = new ApiCredential();
       ApiLogging apiLogging = new ApiLogging();
       apiLogging.setEnableLogging(false);
       apiLogging.setLogExceptions(false);
       apiLogging.setLogSOAPMessages(false);

       String devID = (String)config.get("devId");
        String appID = (String)config.get("appID");
        String certID = (String)config.get("certID");
        String token = (String)config.get("token");
        String apiServerUrl = (String)config.get("apiServerUrl");

       if (token != null) {
           apiCredential.seteBayToken(token);
       } else if (devID != null && appID != null && certID != null) {
           ApiAccount apiAccount = new ApiAccount();
           apiAccount.setApplication(appID);
           apiAccount.setCertificate(certID);
           apiAccount.setDeveloper(devID);
           apiCredential.setApiAccount(apiAccount);
       }
       ApiContext apiContext = new ApiContext();
       apiContext.setApiCredential(apiCredential);
       apiContext.setApiServerUrl(apiServerUrl);
       apiContext.setApiLogging(apiLogging); 
       apiContext.setErrorLanguage("en_US");
       return apiContext;
    }

    public static SiteCodeType getSiteCodeType(String productStoreId, Locale locale, Delegator delegator) {
        Map<String, Object> context = FastMap.newInstance();
        context.put("locale", locale);
        context.put("productStoreId", productStoreId);
        Map<String, Object> config = EbayHelper.buildEbayConfig(context, delegator);
        String siteId = (String)config.get("siteID");
        if (siteId != null) {
            if (siteId.equals("0")) return SiteCodeType.US;
            if (siteId.equals("2")) return SiteCodeType.CANADA;
            if (siteId.equals("3")) return SiteCodeType.UK;
            if (siteId.equals("15")) return SiteCodeType.AUSTRALIA;
            if (siteId.equals("16")) return SiteCodeType.AUSTRIA;
            if (siteId.equals("23")) return SiteCodeType.BELGIUM_FRENCH;
            if (siteId.equals("71")) return SiteCodeType.FRANCE;
            if (siteId.equals("77")) return SiteCodeType.GERMANY;
            if (siteId.equals("100")) return SiteCodeType.E_BAY_MOTORS;
            if (siteId.equals("101")) return SiteCodeType.ITALY;
            if (siteId.equals("123")) return SiteCodeType.BELGIUM_DUTCH;
            if (siteId.equals("146")) return SiteCodeType.NETHERLANDS;
            if (siteId.equals("189")) return SiteCodeType.SPAIN;
            if (siteId.equals("193")) return SiteCodeType.SWITZERLAND;
            if (siteId.equals("196")) return SiteCodeType.TAIWAN;
            if (siteId.equals("201")) return SiteCodeType.HONG_KONG;
            if (siteId.equals("203")) return SiteCodeType.INDIA;
            if (siteId.equals("205")) return SiteCodeType.IRELAND;
            if (siteId.equals("207")) return SiteCodeType.MALAYSIA;
            if (siteId.equals("210")) return SiteCodeType.CANADA_FRENCH;
            if (siteId.equals("211")) return SiteCodeType.PHILIPPINES;
            if (siteId.equals("212")) return SiteCodeType.POLAND;
            if (siteId.equals("216")) return SiteCodeType.SINGAPORE;
            if (siteId.equals("218")) return SiteCodeType.SWEDEN;
            if (siteId.equals("223")) return SiteCodeType.CHINA;
        }
        return SiteCodeType.US;
    }

    public static boolean validatePartyAndRoleType(Delegator delegator, String partyId) {
        GenericValue partyRole = null;
        try {
            if (partyId == null) {
                Debug.logError("Require field partyId.",module);
                return false;
            }
            partyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", "EBAY_ACCOUNT"));
            if (partyRole == null) {
                Debug.logError("Party Id ".concat(partyId).concat("not have roleTypeId EBAY_ACCOUNT"),module);
                return false;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return false;
        }
        return true;
    }

    public static String retriveEbayCategoryIdByPartyId(Delegator delegator, String productCategoryId, String partyId) {
        String ebayCategoryId = null;
        List<GenericValue> productCategoryRoles = null;
        try {
            if (partyId == null) {
                Debug.logError("Require field partyId.",module);
                return ebayCategoryId;
            }
            productCategoryRoles = delegator.findByAnd("ProductCategoryRole", UtilMisc.toMap("productCategoryId", productCategoryId, "partyId", partyId, "roleTypeId", "EBAY_ACCOUNT"));
            if (productCategoryRoles != null && productCategoryRoles.size()>0) {
                for (GenericValue productCategoryRole : productCategoryRoles) {
                    ebayCategoryId = productCategoryRole.getString("comments");
                }
            } else {
                Debug.logInfo("Party Id ".concat(partyId).concat(" Not found productCategoryRole with productCategoryId "+ productCategoryId),module);
                return ebayCategoryId;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        }
        return ebayCategoryId;
    }

    public static boolean createEbayCategoryIdByPartyId(Delegator delegator, String productCategoryId, String partyId, String ebayCategoryId) {
        try {
            if (partyId == null && ebayCategoryId != null) {
                Debug.logError("Require field partyId and ebayCategoryId.",module);
                return false;
            }
            GenericValue productCategoryRole = delegator.makeValue("ProductCategoryRole");
            productCategoryRole.put("productCategoryId",productCategoryId);
            productCategoryRole.put("partyId", partyId);
            productCategoryRole.put("roleTypeId","EBAY_ACCOUNT");
            productCategoryRole.put("fromDate",UtilDateTime.nowTimestamp());
            productCategoryRole.put("comments",ebayCategoryId);
            productCategoryRole.create();
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return false;
        }
        return true;
    }

    public static boolean veriflyCategoryInCatalog(Delegator delegator, List<GenericValue> catalogCategories, String productCategoryId) {
        boolean flag = false;
        try {
            for (GenericValue catalogCategory : catalogCategories) {
                // check in productCatalogCategory first level 0
                if (catalogCategory.containsValue(productCategoryId)) {
                    flag = true;
                    break;
                } else {
                    // check from child category level 1
                    List<GenericValue> productCategoryRollupList = delegator.findByAnd("ProductCategoryRollup",  UtilMisc.toMap("parentProductCategoryId",catalogCategory.getString("productCategoryId")));
                    for (GenericValue productCategoryRollup : productCategoryRollupList) {
                        if (productCategoryRollup.containsValue(productCategoryId)) {
                            flag = true;
                            break;
                        } else {
                            // check from level 2
                            List<GenericValue> prodCategoryRollupList = delegator.findByAnd("ProductCategoryRollup",  UtilMisc.toMap("parentProductCategoryId",productCategoryRollup.getString("productCategoryId")));
                            for (GenericValue prodCategoryRollup : prodCategoryRollupList) {
                                if (prodCategoryRollup.containsValue(productCategoryId)) {
                                    flag = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return false;
        }
        return flag;
    }

    public static Map<String, Object> startEbayAutoPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String autoPrefEnumId = (String) context.get("autoPrefEnumId");
        String serviceName = (String) context.get("serviceName");
        try {
            GenericValue ebayProductPref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId, "autoPrefEnumId", autoPrefEnumId));
            String jobId = ebayProductPref.getString("autoPrefJobId");
            if (UtilValidate.isNotEmpty(jobId)) {
                List<GenericValue> jobs = delegator.findByAnd("JobSandbox", UtilMisc.toMap("parentJobId", jobId, "statusId", "SERVICE_PENDING"));
                if (jobs.size() == 0) {
                    Map<String, Object>inMap = FastMap.newInstance();
                    inMap.put("jobId", jobId);
                    inMap.put("userLogin", userLogin);
                    dispatcher.runSync("resetScheduledJob", inMap);
                }
            }
            if (UtilValidate.isEmpty(ebayProductPref.getString("autoPrefJobId"))) {
                if (UtilValidate.isEmpty(serviceName)) return ServiceUtil.returnError("If you add a new job, you have to add serviec name.");
                /*** RuntimeData ***/
                String runtimeDataId = null;
                GenericValue runtimeData = delegator.makeValue("RuntimeData");
                runtimeData = delegator.createSetNextSeqId(runtimeData);
                runtimeDataId = runtimeData.getString("runtimeDataId");

                /*** JobSandbox ***/
                // create the recurrence
                String infoId = null;
                String jobName = null;
                long startTime = UtilDateTime.getNextDayStart(UtilDateTime.nowTimestamp()).getTime();
                RecurrenceInfo info;
                // run every day when day start
                info = RecurrenceInfo.makeInfo(delegator, startTime, 4, 1, -1);
                infoId = info.primaryKey();
                // set the persisted fields
                GenericValue enumeration = delegator.findByPrimaryKey("Enumeration", UtilMisc.toMap("enumId", autoPrefEnumId));
                    jobName = enumeration.getString("description");
                    if (jobName == null) {
                        jobName = Long.toString((new Date().getTime()));
                    }
                    Map<String, Object> jFields = UtilMisc.<String, Object>toMap("jobName", jobName, "runTime", UtilDateTime.nowTimestamp(),
                        "serviceName", serviceName, "statusId", "SERVICE_PENDING", "recurrenceInfoId", infoId, "runtimeDataId", runtimeDataId);

                // set the pool ID
                jFields.put("poolId", ServiceConfigUtil.getSendPool());

                // set the loader name
                jFields.put("loaderName", JobManager.dispatcherName);
                // create the value and store
                GenericValue jobV;
                jobV = delegator.makeValue("JobSandbox", jFields);
                GenericValue jobSandbox = delegator.createSetNextSeqId(jobV);
                
                ebayProductPref.set("autoPrefJobId", jobSandbox.getString("jobId"));
                ebayProductPref.store();
                
                Map<String, Object>infoData = FastMap.newInstance();
                infoData.put("jobId", jobSandbox.getString("jobId"));
                infoData.put("productStoreId", ebayProductPref.getString("productStoreId"));
                runtimeData.set("runtimeInfo", XmlSerializer.serialize(infoData));
                runtimeData.store();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (SerializeException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            return ServiceUtil.returnError(e.getMessage());
        }catch (RecurrenceInfoException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> stopEbayAutoPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String autoPrefEnumId = (String) context.get("autoPrefEnumId");
        try {
            GenericValue ebayProductPref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId, "autoPrefEnumId", autoPrefEnumId));
            String jobId = ebayProductPref.getString("autoPrefJobId");
            List<GenericValue> jobs = delegator.findByAnd("JobSandbox", UtilMisc.toMap("parentJobId", jobId ,"statusId", "SERVICE_PENDING"));

            Map<String, Object>inMap = FastMap.newInstance();
            inMap.put("userLogin", userLogin);
            for (int index = 0; index < jobs.size(); index++) {
                inMap.put("jobId", jobs.get(index).getString("jobId"));
                dispatcher.runSync("cancelScheduledJob", inMap);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static void mappedPaymentMethods(Map requestParams, String itemPkCateId, Map<String,Object> addItemObject, ItemType item, HashMap attributeMapList) {
        String refName = "itemCateFacade_"+itemPkCateId;
        if (UtilValidate.isNotEmpty(addItemObject) && UtilValidate.isNotEmpty(requestParams)) {
            EbayStoreCategoryFacade cf = (EbayStoreCategoryFacade) addItemObject.get(refName);
            BuyerPaymentMethodCodeType[] paymentMethods = cf.getPaymentMethods();
            if (UtilValidate.isNotEmpty(paymentMethods)) {
                BuyerPaymentMethodCodeType[] tempPayments = new BuyerPaymentMethodCodeType[paymentMethods.length];
                int i = 0;
                for (BuyerPaymentMethodCodeType paymentMethod : paymentMethods) {
                    String pmName = paymentMethod.value();
                    String payPara = (String) requestParams.get("Payments_".concat(pmName));
                    if ("true".equals(payPara)) {
                        tempPayments[i] = paymentMethod;
                        attributeMapList.put(""+pmName, pmName);
                        if ("PayPal".equals(pmName)) {
                            if (UtilValidate.isNotEmpty(requestParams.get("paymentMethodPaypalEmail"))) {
                                item.setPayPalEmailAddress(requestParams.get("paymentMethodPaypalEmail").toString());
                                attributeMapList.put("PaypalEmail", requestParams.get("paymentMethodPaypalEmail").toString());
                            }
                        }
                        i++;
                    }
                }
                item.setPaymentMethods(tempPayments);
            }
        }
    }

    public static void mappedShippingLocations(Map requestParams, ItemType item, ApiContext apiContext, HttpServletRequest request, HashMap attributeMapList) {
        try {
            if (UtilValidate.isNotEmpty(requestParams)) {
                EbayStoreSiteFacade sf = (EbayStoreSiteFacade) EbayEvents.getSiteFacade(apiContext, request);
                Map<SiteCodeType, GeteBayDetailsResponseType> eBayDetailsMap = sf.getEBayDetailsMap();
                GeteBayDetailsResponseType eBayDetails = eBayDetailsMap.get(apiContext.getSite());
                ShippingLocationDetailsType[] shippingLocationDetails = eBayDetails.getShippingLocationDetails();
                if (UtilValidate.isNotEmpty(shippingLocationDetails)) {
                    int i = 0;
                    String[] tempShipLocation = new String[shippingLocationDetails.length];
                    for (ShippingLocationDetailsType shippingLocationDetail : shippingLocationDetails) {
                        String shippingLocation = (String) shippingLocationDetail.getShippingLocation();
                        String shipParam = (String)requestParams.get("Shipping_".concat(shippingLocation));
                        if ("true".equals(shipParam)) {
                            tempShipLocation[i] = shippingLocation;
                            attributeMapList.put(""+shippingLocation, shippingLocation);
                            i++;
                        }
                    }
                    item.setShipToLocations(tempShipLocation);
                }
            }
        } catch(Exception e) {
            Debug.logError(e.getMessage(), module);
        }
    }

    public static Map<String, Object> exportProductEachItem(DispatchContext dctx, Map<String, Object> context) {
        Map<String,Object> result = FastMap.newInstance();
        LocalDispatcher dispatcher = (LocalDispatcher) dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> itemObject = (Map<String, Object>) context.get("itemObject");
        String productListingId = itemObject.get("productListingId").toString();
        AddItemCall addItemCall = (AddItemCall) itemObject.get("addItemCall");
        AddItemRequestType req = new AddItemRequestType();
        AddItemResponseType resp = null;
        try {
            GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            ItemType item = addItemCall.getItem();
            req.setItem(item);
            resp = (AddItemResponseType) addItemCall.execute(req);
            if (resp != null && "SUCCESS".equals(resp.getAck().toString()) || "WARNING".equals(resp.getAck().toString())) {
                String itemId = resp.getItemID();
                String listingXml = addItemCall.getRequestXml().toString();
                Map<String, Object> updateItemMap = FastMap.newInstance();
                updateItemMap.put("productListingId", productListingId);
                updateItemMap.put("itemId", itemId);
                updateItemMap.put("listingXml", listingXml);
                updateItemMap.put("statusId", "ITEM_APPROVED");
                updateItemMap.put("userLogin", userLogin);
                try {
                    dispatcher.runSync("updateEbayProductListing", updateItemMap);
                } catch (GenericServiceException ex) {
                    Debug.logError(ex.getMessage(), module);
                    return ServiceUtil.returnError(ex.getMessage());
                }
            }
            result = ServiceUtil.returnSuccess();
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> setEbayProductListingAttribute(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        HashMap attributeMapList = (HashMap) context.get("attributeMapList");
        String productListingId = (String) context.get("productListingId");
        try {
           List<GenericValue> attributeToClears = delegator.findByAnd("EbayProductListingAttribute", UtilMisc.toMap("productListingId", productListingId));
           for (int clearCount = 0; clearCount < attributeToClears.size(); clearCount++) {
              GenericValue valueToClear = attributeToClears.get(clearCount);
              if (valueToClear != null) {
                 valueToClear.remove();
              }
           }
           Set attributeSet = attributeMapList.entrySet();
           Iterator itr = attributeSet.iterator();
           while (itr.hasNext()) {
             Map.Entry attrMap = (Map.Entry) itr.next();

             if (UtilValidate.isNotEmpty(attrMap.getKey())) {
                 GenericValue ebayProductListingAttribute = delegator.makeValue("EbayProductListingAttribute");
                  ebayProductListingAttribute.set("productListingId", productListingId);
                  ebayProductListingAttribute.set("attrName", attrMap.getKey().toString());
                  ebayProductListingAttribute.set("attrValue", attrMap.getValue().toString());
                  ebayProductListingAttribute.create();
              }
           }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static ItemType prepareAddItem(Delegator delegator, GenericValue attribute) {
        ItemType item = new ItemType();
        try {
            List<GenericValue> attrs = delegator.findByAnd("EbayProductListingAttribute", UtilMisc.toMap("productListingId", attribute.getString("productListingId")));
            AmountType amount = new AmountType();
            AmountType shippingServiceCost = new AmountType();
            PictureDetailsType picture = new PictureDetailsType();
            CategoryType category = new CategoryType();
            ListingDesignerType designer = new ListingDesignerType();
            ShippingDetailsType shippingDetail = new ShippingDetailsType();
            ShippingServiceOptionsType shippingOption = new ShippingServiceOptionsType();
            for (int index = 0; index < attrs.size(); index++) {
                if ("Title".equals(attrs.get(index).getString("attrName"))) {
                    item.setTitle(attrs.get(index).getString("attrValue"));
                } else if ("SKU".equals(attrs.get(index).getString("attrName"))) {
                    item.setSKU(attrs.get(index).getString("attrValue"));
                } else if ("Currency".equals(attrs.get(index).getString("attrName"))) {
                    amount.setCurrencyID(CurrencyCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("Description".equals(attrs.get(index).getString("attrName"))) {
                    item.setDescription(attrs.get(index).getString("attrValue"));
                } else if ("ApplicationData".equals(attrs.get(index).getString("attrName"))) {
                    item.setApplicationData(attrs.get(index).getString("attrValue"));
                } else if ("Country".equals(attrs.get(index).getString("attrName"))) {
                    item.setCountry(CountryCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("PictureURL".equals(attrs.get(index).getString("attrName"))) {
                    String[] pictureUrl = {attrs.get(index).getString("attrValue")};
                    picture.setPictureURL(pictureUrl);
                } else if ("Site".equals(attrs.get(index).getString("attrName"))) {
                    item.setSite(SiteCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("UseTaxTable".equals(attrs.get(index).getString("attrName"))) {
                    item.setUseTaxTable(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("BestOfferEnabled".equals(attrs.get(index).getString("attrName"))) {
                    item.setBestOfferEnabled(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("AutoPayEnabled".equals(attrs.get(index).getString("attrName"))) {
                    item.setAutoPay(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("CategoryID".equals(attrs.get(index).getString("attrName"))) {
                    category.setCategoryID(attrs.get(index).getString("attrValue"));
                } else if ("CategoryLevel".equals(attrs.get(index).getString("attrName"))) {
                    category.setCategoryLevel(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("CategoryName".equals(attrs.get(index).getString("attrName"))) {
                    category.setCategoryName(attrs.get(index).getString("attrValue"));
                } else if ("CategoryParentID".equals(attrs.get(index).getString("attrName"))) {
                    String[] parent = {attrs.get(index).getString("attrValue")};
                    category.setCategoryParentID(parent );
                } else if ("LeafCategory".equals(attrs.get(index).getString("attrName"))) {
                    category.setLeafCategory(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("LSD".equals(attrs.get(index).getString("attrName"))) {
                    category.setLSD(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("ReturnsAcceptedOption".equals(attrs.get(index).getString("attrName"))) {
                    ReturnPolicyType policy = new ReturnPolicyType();
                    policy.setReturnsAcceptedOption(attrs.get(index).getString("attrValue"));
                    item.setReturnPolicy(policy);
                } else if ("LayoutID".equals(attrs.get(index).getString("attrName"))) {
                    designer.setLayoutID(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("ThemeID".equals(attrs.get(index).getString("attrName"))) {
                    designer.setThemeID(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("BuyItNowPrice".equals(attrs.get(index).getString("attrName"))) {
                    amount = new AmountType();
                    amount.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    item.setBuyItNowPrice(amount);
                } else if ("ReservePrice".equals(attrs.get(index).getString("attrName"))) {
                    amount = new AmountType();
                    amount.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    item.setReservePrice(amount);
                } else if ("ListingType".equals(attrs.get(index).getString("attrName"))) {
                    item.setListingType(ListingTypeCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("StartPrice".equals(attrs.get(index).getString("attrName"))) {
                    amount = new AmountType();
                    amount.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    item.setStartPrice(amount);
                } else if ("ShippingService".equals(attrs.get(index).getString("attrName"))) {
                    shippingOption.setShippingService(attrs.get(index).getString("attrValue"));
                } else if ("ShippingServiceCost".equals(attrs.get(index).getString("attrName"))) {
                    shippingServiceCost.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    shippingOption.setShippingServiceCost(shippingServiceCost);
                } else if ("ShippingServiceCostCurrency".equals(attrs.get(index).getString("attrName"))) {
                    shippingServiceCost.setCurrencyID(CurrencyCodeType.valueOf(attrs.get(index).getString("attrValue")));
                    shippingOption.setShippingServiceCost(shippingServiceCost);
                } else if ("ShippingServicePriority".equals(attrs.get(index).getString("attrName"))) {
                    shippingOption.setShippingServicePriority(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("ShippingType".equals(attrs.get(index).getString("attrName"))) {
                    shippingDetail.setShippingType(ShippingTypeCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("VATPercent".equals(attrs.get(index).getString("attrName"))) {
                    VATDetailsType vat = new VATDetailsType();
                    vat.setVATPercent(new Float(attrs.get(index).getString("attrValue")));
                    item.setVATDetails(vat);
                } else if ("Location".equals(attrs.get(index).getString("attrName"))) {
                    item.setLocation(attrs.get(index).getString("attrValue"));
                } else if ("Quantity".equals(attrs.get(index).getString("attrName"))) {
                    item.setQuantity(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("ListingDuration".equals(attrs.get(index).getString("attrName"))) {
                    item.setListingDuration(attrs.get(index).getString("attrValue"));
                } else if ("LotSize".equals(attrs.get(index).getString("attrName"))) {
                    item.setLotSize(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("PostalCode".equals(attrs.get(index).getString("attrName"))) {
                    item.setPostalCode(attrs.get(index).getString("attrValue"));
                } else if ("Title".equals(attrs.get(index).getString("attrName"))) {
                    item.setTitle(attrs.get(index).getString("attrValue"));
                }
                if (category != null) {
                    item.setPrimaryCategory(category);
                }
                if (shippingOption != null) {
                    ShippingServiceOptionsType[] options = {shippingOption};
                    shippingDetail.setShippingServiceOptions(options);
                }
                if (shippingDetail != null) {
                    item.setShippingDetails(shippingDetail);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return null;
        }
        return item;
    }
}