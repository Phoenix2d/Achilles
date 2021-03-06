package info.archinnov.achilles.test.integration.tests.bugs;

import static info.archinnov.achilles.configuration.ConfigurationParameters.EVENT_INTERCEPTORS;
import static info.archinnov.achilles.configuration.ConfigurationParameters.FORCE_TABLE_CREATION;
import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import info.archinnov.achilles.internal.proxy.dirtycheck.DirtyChecker;
import info.archinnov.achilles.persistence.PersistenceManager;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import info.archinnov.achilles.async.AchillesFuture;
import info.archinnov.achilles.embedded.CassandraEmbeddedServerBuilder;
import info.archinnov.achilles.interceptor.Event;
import info.archinnov.achilles.interceptor.Interceptor;
import info.archinnov.achilles.internal.interceptor.AchillesInternalInterceptor;
import info.archinnov.achilles.internal.persistence.operations.EntityProxifier;
import info.archinnov.achilles.persistence.AsyncManager;
import info.archinnov.achilles.persistence.PersistenceManagerFactory;
import info.archinnov.achilles.test.integration.entity.CompleteBean;
import info.archinnov.achilles.test.integration.entity.CompleteBeanTestBuilder;

public class EnsurePostUpdateInterceptorCalledInAsyncIT {

    private EntityProxifier proxifier = EntityProxifier.Singleton.INSTANCE.get();

    @Test
    public void should_call_interceptor_after_async_update() throws Exception {
        // Given
        UpdateAtomicBoolean updateAtomicBoolean = new UpdateAtomicBoolean();
        PersistenceManagerFactory pmf = CassandraEmbeddedServerBuilder
                .withEntities(CompleteBean.class)
                .cleanDataFilesAtStartup(true)
                .withKeyspaceName("interceptor_keyspace_test_async")
                .withAchillesConfigParams(ImmutableMap.of(EVENT_INTERCEPTORS, asList(updateAtomicBoolean), FORCE_TABLE_CREATION, true))
                .buildPersistenceManagerFactory();

        AsyncManager manager = pmf.createAsyncManager();

        CompleteBean bean = CompleteBeanTestBuilder.builder().randomId().name("name").buid();

        AchillesFuture<CompleteBean> insert = manager.insert(bean);
        insert.get();

        // When
        final CompleteBean found = manager.forUpdate(CompleteBean.class, bean.getId());
        manager.update(found);

        // Then
        Thread.sleep(5000);
        assertThat(updateAtomicBoolean.getABoolean().get()).isTrue();
    }

    @Test
    public void should_use_proxy_when_call_interceptor_with_post_update_event() throws Exception {
        // Given
        StoreEntity storeEntity = new StoreEntity();

        PersistenceManagerFactory pmf = CassandraEmbeddedServerBuilder
                .withEntities(CompleteBean.class)
                .cleanDataFilesAtStartup(true)
                .withKeyspaceName("interceptor_keyspace_test_proxy")
                .withAchillesConfigParams(ImmutableMap.of(EVENT_INTERCEPTORS, asList(storeEntity), FORCE_TABLE_CREATION, true))
                .buildPersistenceManagerFactory();

        PersistenceManager manager = pmf.createPersistenceManager();

        CompleteBean bean = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
        manager.insert(bean);

        // When
        final CompleteBean proxy = manager.forUpdate(CompleteBean.class, bean.getId());
        proxy.setName("another name");
        proxy.setAge(33L);
        proxy.setLabel("label");
        manager.update(proxy);

        // Then
        final Set<String> dirtyFields = storeEntity.getDirtyFields();
        assertThat(dirtyFields).containsOnly("name", "age", "label");
        assertThat(proxifier.getInterceptor(proxy).getDirtyMap()).isEmpty();
    }

    private class UpdateAtomicBoolean implements Interceptor<CompleteBean> {

        private AtomicBoolean aBoolean = new AtomicBoolean(false);

        @Override
        public void onEvent(CompleteBean entity) {
            aBoolean.set(true);
        }

        @Override
        public List<Event> events() {
            return ImmutableList.of(Event.POST_UPDATE);
        }

        public AtomicBoolean getABoolean() {
            return aBoolean;
        }
    }

    private class StoreEntity implements AchillesInternalInterceptor<CompleteBean> {

        private Set<String> dirtyFields = new HashSet<>();

        @Override
        public void onEvent(CompleteBean entity) {
            final Map<Method, DirtyChecker> dirtyMap = proxifier.getInterceptor(entity).getDirtyMap();
            this.dirtyFields.addAll(FluentIterable.from(dirtyMap.values()).transform(new Function<DirtyChecker, String>() {
                @Override
                public String apply(DirtyChecker dirtyChecker) {
                    return dirtyChecker.getPropertyMeta().getPropertyName();
                }
            }).toList());
        }

        @Override
        public List<Event> events() {
            return ImmutableList.of(Event.POST_UPDATE);
        }

        public Set<String> getDirtyFields() {
            return dirtyFields;
        }
    }

}
