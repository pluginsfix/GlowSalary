package pluginsfix.glowsalary.storage;

import pluginsfix.glowsalary.domain.SalaryProfile;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SalaryRepository extends AutoCloseable {

    CompletableFuture<SalaryProfile> loadProfile(UUID playerId);

    CompletableFuture<Void> saveProfile(SalaryProfile profile);

    CompletableFuture<Void> saveAll(Collection<SalaryProfile> profiles);

    CompletableFuture<Void> resetProfile(UUID playerId);

    @Override
    void close();
}
