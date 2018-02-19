package com.github.j5ik2o.bank.adaptor.dao

<#assign softDelete=false>
trait ${className}Component extends ComponentSupport with ${className}ComponentSupport {

  import profile.api._

  case class ${className}Record(
<#list primaryKeys as primaryKey>
    ${primaryKey.propertyName}: ${primaryKey.propertyTypeName}<#if primaryKey_has_next>,</#if></#list><#if primaryKeys?has_content>,</#if>
<#list columns as column>
    <#if column.columnName == "status">
        <#assign softDelete=true>
    </#if>
    <#if column.nullable>    ${column.propertyName}: Option[${column.propertyTypeName}]<#if column_has_next>,</#if>
    <#else>    ${column.propertyName}: ${column.propertyTypeName}<#if column_has_next>,</#if>
    </#if>
</#list>
  ) extends <#if softDelete == false>Record<#else>SoftDeletableRecord</#if>

  case class ${className}s(tag: Tag) extends TableBase[${className}Record](tag, "${tableName}")<#if softDelete == true> with SoftDeletableTableSupport[${className}Record]</#if> {
<#list primaryKeys as primaryKey>
    // def ${primaryKey.propertyName} = column[${primaryKey.propertyTypeName}]("${primaryKey.columnName}", O.PrimaryKey)</#list>
<#list columns as column>
    <#if column.nullable>
    def ${column.propertyName} = column[Option[${column.propertyTypeName}]]("${column.columnName}")
    <#else>
    def ${column.propertyName} = column[${column.propertyTypeName}]("${column.columnName}")
    </#if>
</#list>
    override def * = (<#list primaryKeys as primaryKey>${primaryKey.propertyName}<#if primaryKey_has_next>,</#if></#list><#if primaryKeys?has_content>,</#if><#list columns as column>${column.propertyName}<#if column_has_next>,</#if></#list>) <> (${className}Record.tupled, ${className}Record.unapply)
  }

  object ${className}Dao extends TableQuery(${className}s) with DaoSupport[Long, ${className}Record] with ${className}DaoSupport

}
