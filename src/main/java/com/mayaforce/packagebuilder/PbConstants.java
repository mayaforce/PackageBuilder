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

    public static final String DEFAULT_API_VERSION = "60.0"; 
    public static final boolean DEFAULT_INCLUDECHANGEDATA = false;
    // Static values that don't change
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm";
    public static final int DEFAULT_MAXITEMSINPACKAGE = 30000;
    public static final int CONCURRENT_THREADS = 8;

    //To recreate this list: https://developer.salesforce.com/docs/atlas.en-us.api_meta.meta/api_meta/standardvalueset_names.htm 
    // Use regex \d$ to remove footnote numbers. Note that "RoleInTerritory2", ends in a 2 and not a footnote number
    // AddressCountryCode an AddressStateCode are not listed on the documentation page. Manually added. Note that they are incompatible deploying with settings/Address.settings (state/country picklists feature)
    static final String[] STANDARDVALUETYPESARRAY = new String[]{
"AAccreditationRating",
"AccountContactMultiRoles",
"AccountContactRole",
"AccountOwnership",
"AccountRating",
"AccountType",
"AccreditationAccreditingBody",
"AccreditationStatus",
"AccreditationSubType",
"AccreditationType",
"ACInitSumEmployeeType",
"ACInitSumInitiativeType",
"ACISumRecipientCategory",
"ACorruptionInitSumCountry",
"ACorruptionInitSumRegion",
"ACSizeTypeCreativeType",
"ACSizeTypeMediaType",
"ACSizeTypeUnitOfMeasure",
"ACSTypeAppearanceOrder",
"ActivityTimeEnum",
"AddressCountryCode",
"AddressStateCode",
"AdmissionSource",
"AdmissionType",
"AdOpportunityDealType",
"AdQuoteLineSkippable",
"AllergyIntoleranceCategory",
"AllergyIntoleranceSeverity",
"AllergyIntoleranceStatus",
"AllergyIntoleranceType",
"AllergyVerificationStatus",
"AOIAdBleedAmountUom",
"AOIAPPriorityType",
"AOICustomerDayPart",
"AOIMaxFreqInterval",
"AOIMaxUserFreqInterval",
"AOIMediaType",
"AOISponsorshipType",
"AOIUEGoalType",
"AOIUEGoalUnitType",
"AOLConditionRequirements",
"AppealRequestReasonType",
"ApprovedLevelOfCare",
"APTCMediaType",
"AQLAdBleedAmountUom",
"AQLAPPriorityType",
"AQLASPLineStatus",
"AQLConditionRequirements",
"AQLCustomerDayPart",
"AQLMediaType",
"AQLMFrequencyInterval",
"AQLMUFInterval",
"AQLSponsorshipType",
"AQLUEGoalType",
"AQLUEGoalUnitType",
"AQuestionQuestionCategory",
"AReasonAppointmentReason",
"ASAType",
"ASSAdSpaceType",
"ASSCreativeFormatType",
"AssessmentRating",
"AssessmentStatus",
"AssetActionCategory",
"AssetRelationshipType",
"AssetStatus",
"AssociatedLocationType",
"ASSPage",
"ASSPosition",
"ASSProgramRunType",
"ASSPublisherDayPart",
"ASUType",
"ATCSDataType",
"ATCSDisplayType",
"AuthorNoteRecipientType",
"BarrierCodeType",
"BCCertificationType",
"BLicenseJurisdictionType",
"BLicenseVerificationStatus",
"BoardCertificationStatus",
"BusinessLicenseStatus",
"CampaignMemberStatus",
"CampaignStatus",
"CampaignType",
"CardType",
"CareAmbulanceTransReason",
"CareAmbulanceTransType",
"CareBarrierPriority",
"CareBarrierStatus",
"CareBenefitVerifyRequestStatus",
"CareDeterminantPriority",
"CareDeterminantTypeDomain",
"CareDeterminantTypeType",
"CareEpisodeStatus",
"CareEpisodeType",
"CareItemStatus",
"CareItemStatusReason",
"CareMetricTargetType",
"CareObservationCategory",
"CareObservationStatus",
"CarePlanActivityStatus",
"CarePlanAuthorizationType",
"CarePlanDetailDetailType",
"CarePreauthItemLaterality",
"CarePreauthStatus",
"CareProgramEnrolleeStatus",
"CareProgramGoalPriority",
"CareProgramGoalStatus",
"CareProgramProductStatus",
"CareProgramProviderRole",
"CareProgramProviderStatus",
"CareProgramStatus",
"CareProgramTeamMemberRole",
"CareQuantityType",
"CareRegisteredDeviceStatus",
"CareRequestExtensionAmbulanceTransportReason",
"CareRequestExtensionAmbulanceTransportType",
"CareRequestExtensionNursingHomeResidentialStatus",
"CareRequestExtensionRequestType",
"CareRequestExtensionServiceLevel",
"CareRequestMemberGender",
"CareRequestMemberPrognosis",
"CareRequestQuantityType",
"CareRequestReviewerStatus",
"CareSpecialtySpecialtyType",
"CareSpecialtySpecialtyUsage",
"CareTaxonomyTaxonomyType",
"CareTeamStatus",
"CaseContactRole",
"CaseEpisodeSubType",
"CaseEpisodeType",
"CaseOrigin",
"CasePriority",
"CaseReason",
"CaseServicePlanStatus",
"CaseStatus",
"CaseType",
"CBCoverageType",
"CBenefitItemLimitTermType",
"CBItemLimitCoverageLevel",
"CBItemLimitNetworkType",
"CCFALineOfBusiness",
"CCPAdditionalBenefits",
"CCProjectMitigationType",
"CCPStandardsAgencyName",
"CCRDPriority",
"CCreditProjectProjectType",
"CDPresentOnAdmission",
"CEIdentifierIdUsageType",
"CEncounterAdmissionSource",
"CEncounterCategory",
"CEncounterDietPreference",
"CEncounterFacilityStatus",
"CEncounterServiceType",
"CEncounterSpecialCourtesy",
"CEncounterStatus",
"CEpisodeDetailDetailType",
"ChangeRequestBusinessReason",
"ChangeRequestCategory",
"ChangeRequestImpact",
"ChangeRequestPriority",
"ChangeRequestRelatedItemImpactLevel",
"ChangeRequestRiskLevel",
"ChangeRequestStatus",
"ClassRankReportingFormat",
"ClassRankWeightingType",
"ClinicalAlertCategories",
"ClinicalAlertStatus",
"ClinicalCaseType",
"ClinicalDetectedIssueSeverityLevel",
"ClinicalDetectedIssueStatus",
"COComponentValueType",
"COCValueInterpretation",
"CodeSetCodeSetType",
"CommunicationChannel",
"CompanyRelationshipType",
"ConsequenceOfFailure",
"ContactPointAddressType",
"ContactPointUsageType",
"ContactRequestReason",
"ContactRequestStatus",
"ContactRole",
"ContractContactRole",
"ContractLineItemStatus",
"ContractStatus",
"COProcessingResult",
"COValueInterpretation",
"CPAActivityType",
"CPADetailDetailType",
"CPAdverseActionActionType",
"CPAdverseActionStatus",
"CPAgreementAgreementType",
"CPAgreementLineofBusiness",
"CPAProhibitedActivity",
"CPDProblemPriority",
"CPEligibilityRuleStatus",
"CPEnrolleeProductStatus",
"CPEnrollmentCardStatus",
"CPFSpecialtySpecialtyRole",
"CPPRole",
"CProgramProductAvailability",
"CPTemplateProblemPriority",
"CRDDrugAdministrationSetting",
"CRDNameType",
"CRDRequestType",
"CRDStatus",
"CRDStatusReason",
"CRECaseSubStatus",
"CREDocumentAttachmentStatus",
"CREIndependentReviewDetermination",
"CREPriorAuthRequestIdentifier",
"CREPriorDischargeStatus",
"CREReopenRequestOutcome",
"CREReopenRequestType",
"CRERequestOutcome",
"CRIApprovedLevelOfCare",
"CRIClinicalDetermination",
"CRICurrentLevelOfCare",
"CRIDeniedLevelOfCare",
"CRIModifiedLevelOfCare",
"CRIPriority",
"CRIRequestedLevelOfCare",
"CRIRequestType",
"CRReviewerReviewerType",
"CSBundleUsageType",
"CServiceRequestIntent",
"CServiceRequestPriority",
"CServiceRequestStatus",
"CSRequestDetailDetailType",
"CurrentLevelOfCare",
"DChecklistItemStatus",
"DecisionReason",
"DEInclSumDiversityType",
"DEInclSumEmployeeType",
"DEInclSumEmploymentType",
"DEInclSumGender",
"DEISumDiversityCategory",
"DeniedLevelOfCare",
"DiagnosisCodeType",
"DiagnosticSummaryCategory",
"DiagnosticSummaryStatus",
"DigitalAssetStatus",
"DischargeDiagnosisCodeType",
"DIssueDetailType",
"DivrsEquityInclSumLocation",
"DivrsEquityInclSumRace",
"DrugClinicalDetermination",
"DSDDocumentRelationType",
"DSDocumentStage",
"DSummaryDetailDetailType",
"DSummaryUsageType",
"EBSEmployeeBenefitType",
"EBSPercentageCalcType",
"EBSummaryBenefitUsage",
"EBSummaryEmploymentType",
"ECTypeContactPointType",
"EDemographicSumAgeGroup",
"EDemographicSumGender",
"EDemographicSumRegion",
"EDemographicSumReportType",
"EDemographicSumWorkType",
"EDevelopmentSumGender",
"EDSumEmployeeType",
"EDSumEmploymentType",
"EDSumProgramCategory",
"EducationLevel",
"EEligibilityCriteriaStatus",
"EmploymentOccupation",
"EmploymentStatus",
"EngagementAttendeeRole",
"EngagementSentimentEnum",
"EngagementStatusEnum",
"EngagementTypeEnum",
"EnrolleeOptOutReasonType",
"EntitlementType",
"EPSumMarket",
"EPSumPerformanceCategory",
"EPSumPerformanceType",
"EPSumRegion",
"ERCompanyBusinessRegion",
"ERCompanySector",
"EReductionTargetTargetType",
"ERTargetOtherTargetKpi",
"ERTTargetSettingMethod",
"EventSubject",
"EventType",
"FacilityRoomBedType",
"FinalLevelOfCare",
"FinanceEventAction",
"FinanceEventType",
"FiscalYearPeriodName",
"FiscalYearPeriodPrefix",
"FiscalYearQuarterName",
"FiscalYearQuarterPrefix",
"ForecastingItemCategory",
"FreightHaulingMode",
"FtprntAuditApprovalStatus",
"FulfillmentStatus",
"FulfillmentType",
"GADetailDetailType",
"GenderIdentity",
"GoalAssignmentProgressionStatus",
"GoalAssignmentStatus",
"GoalDefinitionCategory",
"GoalDefinitionUsageType",
"GovtFinancialAsstSumType",
"GpaWeightingType",
"GrievanceType",
"HCFacilityLocationType",
"HcpCategory",
"HcpCodeType",
"HealthCareDiagnosisCategory",
"HealthCareDiagnosisCodeType",
"HealthCareDiagnosisGender",
"HealthcareProviderStatus",
"HealthConditionDetailType",
"HealthConditionSeverity",
"HealthConditionStatus",
"HealthConditionType",
"HealthDiagnosticStatus",
"HFNetworkGenderRestriction",
"HFNetworkPanelStatus",
"HPayerNetworkNetworkType",
"HPayerNwkLineOfBusiness",
"HPFGenderRestriction",
"HPFTerminationReason",
"HProviderNpiNpiType",
"HProviderProviderClass",
"HProviderProviderType",
"HPSpecialtySpecialtyRole",
"HSActionLogActionStatus",
"IaApplnStatus",
"IaAuthCategory",
"IaInternalStatus",
"IAItemStatus",
"IARejectionReason",
"IAServiceType",
"IdeaCategory",
"IdeaMultiCategory",
"IdeaStatus",
"IdeaThemeStatus",
"IdentifierIdUsageType",
"IFnolChannel",
"IncidentCategory",
"IncidentImpact",
"IncidentPriority",
"IncidentRelatedItemImpactLevel",
"IncidentRelatedItemImpactType",
"IncidentReportedMethod",
"IncidentStatus",
"IncidentSubCategory",
"IncidentType",
"IncidentUrgency",
"Industry",
"InterventionCodeType",
"IPCancelationReasonType",
"IPCBenefitPaymentFrequency",
"IPCCategory",
"IPCCategoryGroup",
"IPCDeathBenefitOptionType",
"IPCIncomeOptionType",
"IPCLimitRange",
"IPolicyAuditTerm",
"IPolicyChangeSubType",
"IPolicyChangeType",
"IPolicyChannel",
"IPolicyPlanTier",
"IPolicyPlanType",
"IPolicyPolicyType",
"IPolicyPremiumCalcMethod",
"IPolicyPremiumFrequency",
"IPolicyPremiumPaymentType",
"IPolicyStatus",
"IPolicySubStatusCode",
"IPolicyTerm",
"IPolicyTransactionStatus",
"IPolicyTransactionType",
"IPOwnerPOwnerType",
"IPParticipantRole",
"IPPRelationshipToInsured",
"LeadSource",
"LeadStatus",
"LicenseClassType",
"LineOfAuthorityType",
"LocationType",
"LPIApplnCategory",
"LPIApplnStatus",
"MCGeographicalCoverage",
"MChannelPricingCategory",
"MCPeriodicalType",
"MCPublicationFrequency",
"MediaChannelMediaType",
"MedicationCategoryEnum",
"MedicationDispenseMedAdministrationSettingCategory",
"MedicationDispenseStatus",
"MedicationDispenseSubstitutionReason",
"MedicationDispenseSubstitutionType",
"MedicationStatementStatus",
"MedicationStatus",
"MedReviewRepresentativeType",
"MedTherapyReviewSubtype",
"MemberPlanPrimarySecondaryTertiary",
"MemberPlanRelToSub",
"MemberPlanStatus",
"MemberPlanVerificStatus",
"MilitaryService",
"ModifiedCareCodeType",
"ModifiedDiagnosisCodeType",
"ModifiedDrugCodeType",
"ModifiedLevelOfCare",
"MRequestPriority",
"MRequestStatus",
"MRequestTherapyDuration",
"MRequestType",
"MStatementDeliverySetting",
"MStatementDetailType",
"OcrService",
"OcrStatus",
"OIncidentSummaryHazardType",
"OISCorrectiveActionType",
"OISummaryIncidentSubtype",
"OISummaryIncidentType",
"OISummaryPenaltyType",
"OpportunityCompetitor",
"OpportunityStage",
"OpportunityType",
"OrderItemSummaryChgRsn",
"OrderStatus",
"OrderSummaryRoutingSchdRsn",
"OrderSummaryStatus",
"OrderType",
"ParProvider",
"PartnerRole",
"PartyProfileCountryofBirth",
"PartyProfileEmploymentType",
"PartyProfileFundSource",
"PartyProfileGender",
"PartyProfileResidentType",
"PartyProfileReviewDecision",
"PartyProfileRiskType",
"PartyProfileStage",
"PartyScreeningStepType",
"PartyScreeningSummaryStatus",
"PatientImmunizationStatus",
"PEFEFctrDataSourceType",
"PersonEmploymentType",
"PersonLanguageLanguage",
"PersonLanguageSpeakingProficiencyLevel",
"PersonLanguageWritingProficiencyLevel",
"PersonNameNameUsageType",
"PersonVerificationStatus",
"PHealthReactionSeverity",
"PIdentityVerificationResult",
"PIdentityVerificationStatus",
"PIVerificationStepStatus",
"PIVerificationStepType",
"PIVerificationVerifiedBy",
"PIVOverriddenResult",
"PIVResultOverrideReason",
"PIVSVerificationDecision",
"PlaceOfService",
"PlanBenefitStatus",
"PMDDosageDefinitionType",
"PMDosageDosageAmountType",
"PMDosageRateType",
"PMPDetailDetailType",
"PMPOutcome",
"PMPStatus",
"PPCreditScoreProvider",
"PPPrimaryIdentifierType",
"PProfileAddressAddressType",
"PProfileCountryOfDomicile",
"PProfileEmploymentIndustry",
"PProfileNationality",
"PProfileOffBoardingReason",
"PProfileRiskRiskCategory",
"PPROverridenRiskCategory",
"PPTaxIdentificationType",
"PrimaryCitizenshipType",
"ProblemCategory",
"ProblemDefinitionCategory",
"ProblemDefinitionPriority",
"ProblemDefinitionUsageTypeEnum",
"ProblemImpact",
"ProblemPriority",
"ProblemRelatedItemImpactLevel",
"ProblemRelatedItemImpactType",
"ProblemStatus",
"ProblemSubCategory",
"ProblemUrgency",
"ProcessExceptionCategory",
"ProcessExceptionPriority",
"ProcessExceptionSeverity",
"ProcessExceptionStatus",
"ProdRequestLineItemStatus",
"Product2Family",
"ProductLineEnum",
"ProductRequestStatus",
"ProgressionCriteriaMet",
"PScreeningStepResultCode",
"PScreeningStepStatus",
"PSSResultOverrideReason",
"PSStepMatchedFieldList",
"PSSummaryScreenedBy",
"PSSummaryScreeningDecision",
"PurchaserPlanAffiliation",
"PurchaserPlanStatus",
"PurchaserPlanType",
"QuantityUnitOfMeasure",
"QuestionOrigin",
"QuickTextCategory",
"QuickTextChannel",
"QuoteStatus",
"ReceivedDocumentDirection",
"ReceivedDocumentOcrStatus",
"ReceivedDocumentPriority",
"ReceivedDocumentStatus",
"RegAuthCategory",
"RegulatoryBodyType",
"ReopenReason",
"RequestedCareCodeType",
"RequestedDrugCodeType",
"RequestedLevelOfCare",
"RequesterType",
"RequestingPractitionerLicense",
"RequestingPractitionerSpecialty",
"ResidenceStatusType",
"ResourceAbsenceType",
"ReturnOrderLineItemProcessPlan",
"ReturnOrderLineItemReasonForRejection",
"ReturnOrderLineItemReasonForReturn",
"ReturnOrderLineItemRepaymentMethod",
"ReturnOrderShipmentType",
"ReturnOrderStatus",
"RoleInTerritory2",
"SalesAgreementStatus",
"SalesTeamRole",
"Salutation",
"SAppointmentGroupStatus",
"ScienceBasedTargetStatus",
"SContributionSumCategory",
"Scope3CrbnFtprntStage",
"ScopeCrbnFtprntStage",
"ScorecardMetricCategory",
"ServiceAppointmentStatus",
"ServiceContractApprovalStatus",
"ServicePlanTemplateStatus",
"ServicingPractitionerLicense",
"ServicingPractitionerSpecialty",
"ServTerrMemRoleType",
"ShiftStatus",
"SocialContributionSumType",
"SocialPostClassification",
"SocialPostEngagementLevel",
"SocialPostReviewedStatus",
"SolutionStatus",
"SourceBusinessRegion",
"StatusReason",
"StnryAssetCrbnFtprntStage",
"StnryAssetWaterFtprntStage",
"StnryAstCrbnFtAllocStatus",
"StnryAstCrbnFtDataGapSts",
"StnryAstEvSrcStnryAstTyp",
"SupplierClassification",
"SupplierEmssnRdctnCmtTypev",
"SupplierReportingScope",
"SupplierTier",
"SustainabilityScorecardStatus",
"TaskPriority",
"TaskStatus",
"TaskSubject",
"TaskType",
"TCDDetailType",
"TCPriority",
"TCStatus",
"TCStatusReason",
"TopicFailureReasonEnum",
"TopicProcessStatusEnum",
"TrackedCommunicationType",
"TypesOfIntervention",
"UnitOfMeasure",
"UnitOfMeasureType",
"VehicleAstCrbnFtprntStage",
"VehicleType",
"WasteDisposalType",
"WasteFootprintStage",
"WasteType",
"WorkOrderLineItemPriority",
"WorkOrderLineItemStatus",
"WorkOrderPriority",
"WorkOrderStatus",
"WorkStepStatus",
"WorkTypeDefApptType",
"WorkTypeGroupAddInfo"
    };

    static final String[] ADDITIONALTYPESTOADD = new String[]{"FeatureParameterBoolean", "FeatureParameterDate", "FeatureParameterInteger"
    };
    static final String[] ITEMSTOINCLUDEWITHPROFILESPERMSETS = new String[]{"ApexClass", "CustomApplication", "CustomField", "CustomObject", "CustomTab", "ExternalDataSource", "RecordType", "ApexPage"};
    static final String[] SPECIALTREATMENTPERMISSIONTYPES = new String[]{"Profile", "PermissionSet"};

}
