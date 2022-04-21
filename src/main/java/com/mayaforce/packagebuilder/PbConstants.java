/*
 * Copyright 2021 EricHaycraft.
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
package com.mayaforce.packagebuilder;

/**
 *
 * @author EricHaycraft
 */
public class PbConstants {

    public static final String DEFAULT_API_VERSION = "54.0";
    public static final boolean DEFAULT_INCLUDECHANGEDATA = false;
    // Static values that don't change
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm";
    public static final int DEFAULT_MAXITEMSINPACKAGE = 30000;
    public static final int CONCURRENT_THREADS = 8;
    
    //To recreate this list: https://developer.salesforce.com/docs/atlas.en-us.api_meta.meta/api_meta/standardvalueset_names.htm 
    // AddressCountryCode an AddressStateCode are not listed on the documentation page. Manually added. Note that they are incompatible deploying with settings/Address.settings (state/country picklists feature)
    static final String[] STANDARDVALUETYPESARRAY = new String[]{"AccountContactMultiRoles", "AccountContactRole", "AccountOwnership", "AccountRating", "AccountType", "AssetStatus", "CampaignMemberStatus", "CampaignStatus", "CampaignType", "CareItemStatus", "CaseContactRole", "CaseOrigin", "CasePriority", "CaseReason", "CaseStatus", "CaseType", "ContactRole", "ContractContactRole", "ContractStatus", "EntitlementType", "EventSubject", "EventType", "FiscalYearPeriodName", "FiscalYearPeriodPrefix", "FiscalYearQuarterName", "FiscalYearQuarterPrefix", "FulfillmentStatus", "FulfillmentType", "IdeaCategory", "IdeaMultiCategory", "IdeaStatus", "IdeaThemeStatus", "Industry", "LeadSource", "LeadStatus", "OpportunityCompetitor", "OpportunityStage", "OpportunityType", "OrderItemSummaryChgRsn", "OrderStatus", "OrderSummaryRoutingSchdRsn", "OrderSummaryStatus", "OrderType", "PartnerRole", "ProcessExceptionCategory", "ProcessExceptionPriority", "ProcessExceptionSeverity", "ProcessExceptionStatus", "Product2Family", "QuestionOrigin", "QuickTextCategory", "QuickTextChannel", "QuoteStatus", "ResourceAbsenceType", "ReturnOrderLineItemProcessPlan", "ReturnOrderLineItemReasonForRejection", "ReturnOrderLineItemReasonForReturn", "ReturnOrderLineItemRepaymentMethod", "ReturnOrderShipmentType", "ReturnOrderStatus", "RoleInTerritory2", "SalesTeamRole", "Salutation", "ServiceAppointmentStatus", "ServiceContractApprovalStatus", "ServTerrMemRoleType", "SocialPostClassification", "SocialPostEngagementLevel", "SocialPostReviewedStatus", "SolutionStatus", "TaskPriority", "TaskStatus", "TaskSubject", "TaskType", "WorkOrderLineItemStatus", "WorkOrderPriority", "WorkOrderStatus", "WorkTypeDefApptType", "WorkTypeGroupAddInfo", "AddressCountryCode", "AddressStateCode"};
    
    static final String[] ADDITIONALTYPESTOADD = new String[]{"CustomLabel", "AssignmentRule", "BusinessProcess", "CompactLayout", "CustomField", "FieldSet", "Index", "ListView", "NamedFilter", "RecordType", "SharingReason", "ValidationRule", "WebLink", // CustomObject components
    "WorkflowActionReference", "WorkflowAlert", "WorkflowEmailRecipient", "WorkflowFieldUpdate", "WorkflowFlowAction", "WorkflowFlowActionParameter", // Workflow components
    "WorkflowKnowledgePublish", "WorkflowOutboundMessage", "WorkflowRule", "WorkflowTask", "WorkflowTimeTrigger" // Workflow components
    };
    static final String[] ITEMSTOINCLUDEWITHPROFILESPERMSETS = new String[]{"ApexClass", "CustomApplication", "CustomField", "CustomObject", "CustomTab", "ExternalDataSource", "RecordType", "ApexPage"};
    static final String[] SPECIALTREATMENTPERMISSIONTYPES = new String[]{"Profile", "PermissionSet"};
    
}
