<%@ include file="/WEB-INF/template/include.jsp"%>
<openmrs:require privilege="Manage Data Exports" otherwise="/login.htm" redirect="/module/formdataexport/formDataExport.list" />
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>
<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/scripts/jquery/jquery-1.3.2.min.js" />
<script type="text/javascript">
	var $j = jQuery.noConflict();
</script>
<script src='<%= request.getContextPath() %>/dwr/interface/FormDataExprtDWRUtil.js'></script>
<h2> 
	<spring:message code="formdataexport.FormDataExport.title" />
</h2>

<script type="text/javascript">
	function setupSections(item){
		var sections = document.getElementById("sections");
		$j(sections).empty();
	    if (item.value != ""){
			FormDataExprtDWRUtil.getFormSections(item.value, function(ret){
				if (ret.length > 0)
			    	$j(sections).append($j('<option value="">CHOOSE A SECTION (OPTIONAL)</option>'));
				for (var i = 0 ; i < ret.length; i++){
					$j(sections).append($j('<option value="'+ret[i].index+'">section '+ret[i].index+'. '+ret[i].name+'</option>'));
				}
			});
		} else {
			
		}		
	}
	
	function checkFirstList(input){
		if (input.id=='first'){
			document.getElementById('last').checked = false;
		} else {
			document.getElementById('first').checked = false;
		}
	}
	
	function isFormSelected(){
		var formId = document.getElementById("formId");
		if (formId.value != "")
			return true;
		else
			return false;
	}
</script>

<form method="post" onsubmit="return isFormSelected();">
	<b class="boxHeader">Select Form</b>
	<div class="box">
		<table>
			<tr>
				<td nowrap><spring:message code="formdataexport.FormDataExport.selectForm"/></td>
				<td> &nbsp;&nbsp;</td>
				<td>
					<select name="formId" onChange="setupSections(this);" id="formId">			
						<option value="" SELECTED></option>	
						<c:forEach var="form" items="${formList}">
							<option value="${form.formId}">${form.name}</option>
						</c:forEach>
					</select>
				</td>
			</tr> 
			<tr>
				<td nowrap>Sections In This Form (Optional)</td>
				<td> &nbsp;&nbsp;</td>
				<td>
					<select id="sections" name="section">
						<option value=""></option>
					</select>
				</td>
			</tr> 
			
			<tr>
				<td nowrap>Between (Optional)</td>
				<td> &nbsp;&nbsp;</td>
		    	<td>
		    		Start Date: <input type="text" id="startDate" name="startDate" onClick="showCalendar(this)"> &nbsp; End Date: <input type="text"  id="endDate" name="endDate" onClick="showCalendar(this)">
		    	</td>
		    </tr>
			
			
 			<tr>
				<td nowarp>Select a Cohort Definition (Optional)</td>
				<td> &nbsp;&nbsp;</td>
				<td>
					<select name="cohortUuid">				
						<option value=""></option>
						<c:forEach var="cohort" items="${cohorts}">
							<option value="${cohort.uuid}">${cohort.name}</option>
						</c:forEach>
					</select>
				</td>
			</tr>
			
			
			<tr>
				<td>Include Extra Columns (Optional)</td>
				<td> &nbsp;&nbsp;</td>
				<td>
					<input type="checkbox" name="extraColumn" value="valueModifier" />Observation Value Modifier<br/>
					<input type="checkbox" name="extraColumn" value="accessionNumber" />Accession Number<br/>
					<input type="checkbox" name="extraColumn" value="comment" />Observation Comments<br/>
					
				</td>
			</tr>
			<tr>
				<td>First, Last, Quantity of a form for a given patient (Optional)</td>
				<td> &nbsp;&nbsp;</td>
				<td>
					First:<input type="checkbox" id="first" name="firstLast" value="first" onmouseup="checkFirstList(this);"/>&nbsp;&nbsp;&nbsp;&nbsp;
					Last:<input type="checkbox" id="last" name="firstLast" value="last" onmouseup="checkFirstList(this);"/>&nbsp;&nbsp;&nbsp;
					Quantity: <input type="text" name="quantity" style="width:25px;"/>
				</td>
			</tr> 
			

			<tr>
				<td align="left" colspan="3">
					<br/>&nbsp;&nbsp;&nbsp;
					<input type="submit" class="smallButton" value='Export' />				
				</td>
			</tr>

		</table>
	</div>
</form>

<br/>

<%@ include file="/WEB-INF/template/footer.jsp"%>
