package mil.nga.giat.mage.dagger;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import dagger.android.support.DaggerApplication;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.dagger.contributor.ActivityContributorModule;
import mil.nga.giat.mage.dagger.contributor.FragmentContributorModule;
import mil.nga.giat.mage.dagger.contributor.ServiceContributorModule;
import mil.nga.giat.mage.dagger.module.ContextModule;
import mil.nga.giat.mage.dagger.module.PreferencesModule;

@Singleton
@Component(modules = {
        AndroidSupportInjectionModule.class,
        ContextModule.class,
        PreferencesModule.class,
        ActivityContributorModule.class,
        FragmentContributorModule.class,
        ServiceContributorModule.class})
public interface MageComponent extends AndroidInjector<DaggerApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(MageApplication application);

        MageComponent build();
    }
}
