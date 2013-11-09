package github.chenxh.core.dao;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.orm.hibernate3.HibernateTransactionManager;

public interface IHibernateDao {

    /**
     * 获取一条记录
     * 
     * @param <T>
     * @param type
     * @param id
     * @return null is id is null, or no record is found
     * @author chenxh 2013-11-9
     */
    public abstract <T> T get(Class<T> type, Serializable id);

    /**
     * 删除一条记录
     * 
     * @param entity
     * @author chenxh 2013-11-9
     */
    public abstract void delete(Object entity);

    /**
     * 使用 hql 批量删除
     * 
     * @param entities
     * @author chenxh 2013-11-9
     */
    public abstract void deleteAll(final Collection<Object> entities);

    
    /**
     * 获取当前数据源的事务
     * @author chenxh 2013-11-9
     */
    public HibernateTransactionManager getTransactionManager();

    public void deleteById(Class<?> type, Serializable id);

    public void deleteAllById(Class<?> type, Serializable[] id);

}