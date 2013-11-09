package github.chenxh.core.dao;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * 基本数据操作
 * 
 * @author chenxh chenxiuheng@gmail.com
 */
public class BaseCRUDDao extends HibernateDaoSupport implements IHibernateDao {
    final protected Logger logger = LoggerFactory.getLogger(getClass());



    /* (non-Javadoc)
     * @see github.chenxh.core.dao.IHibernateDao#get(java.lang.Class, java.io.Serializable)
     */
    public <T> T get(Class<T> type, Serializable id) {
        if (null == id) {
            return null;
        }

        T v = getHibernateTemplate().get(type, id);
        logger.debug("get [{}] by [{}]", v, id);

        return v;
    }

    /* (non-Javadoc)
     * @see github.chenxh.core.dao.IHibernateDao#delete(java.lang.Object)
     */
    public void delete(Object entity) {
        if (null != entity) {
            getHibernateTemplate().delete(entity);
        }
    }
    
    /* (non-Javadoc)
     * @see github.chenxh.core.dao.IHibernateDao#deleteById(java.lang.Class, java.io.Serializable)
     */
    public void deleteById(Class<?> type, Serializable id) {
        Object entity = getHibernateTemplate().get(type, id);
        if (null != entity) {
            delete(entity);
        }
    }
    
    /* (non-Javadoc)
     * @see github.chenxh.core.dao.IHibernateDao#deleteAllById(java.lang.Class, java.io.Serializable[])
     */
    public void deleteAllById(Class<?> type, Serializable[] id) {
        if (null == id) {
            return;
        }

        ArrayList<Object> entites = new ArrayList<Object>();

        for (int i = 0; i < id.length; i++) {
            Object entity = getHibernateTemplate().get(type, id);
            if (null != entity) {
                entites.add(entity);
            }
        }
        
        if (!entites.isEmpty()) {
            getHibernateTemplate().deleteAll(entites);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("delete{}  0 rows by id {}", type, Arrays.toString(id));
            }
        }
    }
    
    

    /* (non-Javadoc)
     * @see github.chenxh.core.dao.IHibernateDao#deleteAll(java.util.Collection)
     */
    public void deleteAll(final Collection<Object> entities) {
        if (null == entities || entities.isEmpty()) {
            return;
        }

        getHibernateTemplate().executeWithNativeSession(
                new HibernateCallback<Object>() {
                    public Object doInHibernate(Session paramSession)
                            throws HibernateException, SQLException {
                        for (Object object : entities) {
                            if (null != object) {
                                paramSession.delete(object);
                            }
                        }
                        return null;
                    }
                });
    }

    /**
     * HQL 查询
     * 
     * @param <T>
     * @param hql
     * @return
     * @author chenxh 2013-11-9
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> findByHql(String hql) {
        return getHibernateTemplate().find(hql);
    }

    /**
     * HQL 查询
     * 
     * @param <T>
     * @param hql
     * @param param
     * @return
     * @author chenxh 2013-11-9
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> findByHql(String hql, Object param) {
        return getHibernateTemplate().find(hql, param);
    }

    /**
     * HQL 查询
     * 
     * @param <T>
     * @param hql
     * @param param
     * @return
     * @author chenxh 2013-11-9
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> findByHql(String hql, Object... param) {
        return getHibernateTemplate().find(hql, param);
    }

    /**
     * JDBC 获取第一条记录
     * 
     * @param <T>
     * @param sql
     * @return
     * @author chenxh 2013-11-9
     */
    @SuppressWarnings("unchecked")
    protected <T> T queryFirst(String sql) {
        List<T> rst = query(sql, Collections.EMPTY_LIST, null);

        if (!rst.isEmpty()) {
            return rst.get(0);
        } else {
            return null;
        }
    }

    /**
     * HQL 查询
     * 
     * @param <T>
     * @param sql
     * @param params
     * @param mapper
     * @return
     * @author chenxh 2013-11-9
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> query(String sql, List<Object> params,
            RowMapper<T> mapper) {
        // create rowmapper
        mapper = (RowMapper<T>) (null != mapper ? mapper : defaultRowMapper);
        
        // params
        Object[] args = ((null == params || params.isEmpty()) ? params
                .toArray() : ArrayUtils.EMPTY_OBJECT_ARRAY);

        // execute
        return getJdbcTemplate().query(sql, args, mapper);
    }

    final static private RowMapper<?> defaultRowMapper;
    static{
        defaultRowMapper = new RowMapper<Object>() {
            public Object mapRow(ResultSet paramResultSet, int paramInt)
            throws SQLException {
                return null;
            }
        };
    }
    
    private JdbcTemplate jdbcTemplate;
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public final void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    HibernateTransactionManager transactionManager;
    public final HibernateTransactionManager getTransactionManager() {
        return transactionManager;
    }


    public final void setTransactionManager(HibernateTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
