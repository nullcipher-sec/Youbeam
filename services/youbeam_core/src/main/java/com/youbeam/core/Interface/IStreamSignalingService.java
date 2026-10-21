package com.youbeam.core.Interface;

import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.StreamOfferRequest;
import com.youbeam.core.model.response.StreamAnswerResponse;

/**
 * Señalización WebRTC: intermediario entre el navegador y el Streaming Server.
 */
public interface IStreamSignalingService {

    StreamAnswerResponse forwardOffer(StreamOfferRequest request) throws ControllerException;
}
