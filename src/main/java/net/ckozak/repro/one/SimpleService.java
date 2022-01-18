package net.ckozak.repro.one;

import com.google.common.util.concurrent.ListenableFuture;
import com.palantir.dialogue.DialogueService;
import com.palantir.dialogue.HttpMethod;
import com.palantir.dialogue.annotations.Request;

@DialogueService(SimpleServiceDialogueServiceFactory.class)
public interface SimpleService {

    @Request(method = HttpMethod.POST, path = "/ping")
    ListenableFuture<SimpleResponse> ping();
}