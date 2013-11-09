package github.chenxh.core.dao;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * 基本数据操作
 * 
 * @author chenxh chenxiuheng@gmail.com
 */
public class BaseCRUDDao extends HibernateDaoSupport
        implements
            ApplicationContextAware {
    final protected Logger logger = LoggerFactory.getLogger(getClass());

    public JdbcTemplate getJdbcTemplate() {
        JdbcDaoSupport jdbc = (JdbcDaoSupport) context.getBean("JDBCDao");
        return jdbc.getJdbcTemplate();
    }

    private ApplicationContext context;
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.context = applicationContext;
    }

    /**
     * 获取一条记录
     * 
     * @param <T>
     * @param type
     * @param id
     * @return null is id is null, or no record is found
     * @author chenxh 2013-11-9
     */
    public <T> T get(Class<T> type, Serializable id) {
        if (null == id) {
            return null;
        }

        T v = getHibernateTemplate().get(type, id);
        logger.debug("get [{}] by [{}]", v, id);

        return v;
    }

    /**
     * 删除一条记录
     * 
     * @param entity
     * @author chenxh 2013-11-9
     */
    public void delete(Object entity) {
        if (null != entity) {
            getHibernateTemplate().delete(entity);
        }
    }

    /**
     * 使用 hql 批量删除 
     * @param entities
     * @author chenxh 2013-11-9
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
     * @param <T>
     * @param sql
     * @param params
     * @param mapper
     * @return
     * @author chenxh 2013-11-9
     */
    protected <T> List<T> query(String sql, List<Object> params,
            RowMapper<T> mapper) {
        rowMapper = (null != rowMapper ? rowMapper : getDefaultRowMapper());
        Object[] args = ((null == params || params.isEmpty()) ? params
                .toArray() : ArrayUtils.EMPTY_OBJECT_ARRAY);
        
        return getJdbcTemplate().query(sql, args, mapper);
    }

    private RowMapper<?> rowMapper;
    private <T> RowMapper<?> getDefaultRowMapper() {
        if (rowMapper == null) {
            rowMapper = new RowMapper<Object>() {
                public Object mapRow(ResultSet paramResultSet, int paramInt)
                        throws SQLException {
                    return null;
                }
            };
        }

        return  rowMapper;
    }
}
