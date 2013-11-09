/*
 * $Id: AbstractCRUDAction.java 1400220 2012-10-19 18:49:39Z jogep $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package github.chenxh.core.action;

import github.chenxh.core.dao.IHibernateDao;

import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionSupport;

/**
 * 自动集成增删改查操作，
 * 
 * 数据操作使用事务
 * 
 * @author chenxh chenxiuheng@gmail.com
 */
public abstract class AbstractCRUDAction<T> extends ActionSupport {

    /**   */
    private static final long serialVersionUID = 1L;


    protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());


    private Class<T> type;
    public AbstractCRUDAction(Class<T> type) {
        this.type = type;
        
        logger.debug("create CRUD action for [{}]", type);
    }

    protected abstract IHibernateDao getDao();
    
    // -------------------------------------------------- 
    //  根据 Id 获取记录
    // --------------------------------------------------
    private String entityId;
    private T entity;
    protected String get() {
        entity = getDao().get(type, entityId);
        
        return SUCCESS;
    }


    public final void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public final T getEntity() {
        return this.entity;
    }
    
    // ------------------------------------------------
    // 根据Id批量删除
    // ------------------------------------------------
    private String[] toDelete;
    public void setToDelete(String[] toDelete) {
        this.toDelete = toDelete;
    }
    protected String delete(){
        getDao().deleteAllById(type, toDelete);
        
        return SUCCESS;
    }
    
    
}
