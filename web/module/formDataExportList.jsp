<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Manage Data Exports" otherwise="/login.htm" redirect="/module/formdataexport/formDataExport.list" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="formdataexport.FormDataExport.title" />
</h2>

<script type="text/javascript">

</script>

<form method="post">
	<b class="boxHeader"><spring:message code="formdataexport.FormDataExport.title" /></b>
	<div class="box">
		<table>
			<tr>
				<td><spring:message code="formdataexport.FormDataExport.selectForm"/></td>
				<td>
					<select name="formId">				
						<c:forEach var="form" items="${formList}">
							<option value="${form.formId}">${form.name}</option>
						</c:forEach>
					</select>
				</td>
			</tr>
			
<!--  			
			<tr>
				<td><spring:message code="formdataexport.FormDataExport.selectCohort"/></td>
				<td>
					<select name="cohortId">				
						<option value="-1">All patients</option>
						<option value="0">All patients with encounters of selected form</option>
						<c:forEach var="cohort" items="${cohortList}">
							<option value="${cohort.cohortId}">${cohort.name}</option>
						</c:forEach>
					</select>
				</td>
			</tr>
-->			
			<!-- 
			
			<tr>
				<td><spring:message code="formdataexport.FormDataExport.extraColumns"/></td>
				<td>
					<input type="checkbox" name="extraColumn" value="obsDatetime" /><spring:message code="DataExport.conceptExtra.obsDatetime"/>
					<input type="checkbox" name="extraColumn" value="location" /><spring:message code="DataExport.conceptExtra.location"/>
					<input type="checkbox" name="extraColumn" value="comment" /><spring:message code="DataExport.conceptExtra.comment"/>
					<input type="checkbox" name="extraColumn" value="encounterType" /><spring:message code="DataExport.conceptExtra.encounterType"/>
					<input type="checkbox" name="extraColumn" value="provider" /><spring:message code="DataExport.conceptExtra.provider"/>
				</td>
			</tr>
			
			
			-->

			<tr>
				<td align="right" colspan="2">
					<input type="submit" class="smallButton" value='<spring:message code="formdataexport.FormDataExport.export" />' />				
				</td>
			</tr>

		</table>
	</div>
</form>

<br/>

<%@ include file="/WEB-INF/template/footer.jsp"%>
