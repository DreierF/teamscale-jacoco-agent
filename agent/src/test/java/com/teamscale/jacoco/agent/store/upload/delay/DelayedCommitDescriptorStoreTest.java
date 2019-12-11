package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.util.InMemoryStore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorStoreTest {

	@Test
	public void shouldStoreToCacheIfCommitIsNotKnown() {
		InMemoryStore cache = new InMemoryStore();
		InMemoryStore destination = new InMemoryStore();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, cache);

		store.store("xml1");

		assertThat(cache.getXmls()).containsExactly("xml1");
		assertThat(destination.getXmls()).isEmpty();
	}

	@Test
	public void shouldStoreToDestinationIfCommitIsKnown() {
		InMemoryStore cache = new InMemoryStore();
		InMemoryStore destination = new InMemoryStore();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, cache);

		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		store.store("xml1");

		assertThat(cache.getXmls()).isEmpty();
		assertThat(destination.getXmls()).containsExactly("xml1");
	}

	@Test
	public void shouldAsynchronouslyStoreToDestinationOnceCommitIsKnown() throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		InMemoryStore cache = new InMemoryStore();
		InMemoryStore destination = new InMemoryStore();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, cache, executor);

		store.store("xml1");
		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(cache.getXmls()).isEmpty();
		assertThat(destination.getXmls()).containsExactly("xml1");
	}

}