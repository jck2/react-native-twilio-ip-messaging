package com.bradbumbalough.RCTTwilioChat;

import android.support.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.twilio.chat.Channel;
import com.twilio.chat.ChannelListener;
import com.twilio.chat.Constants;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.Channels;
import com.twilio.chat.Member;
import com.twilio.chat.Message;
import com.twilio.chat.CallbackListener;

import java.util.HashMap;
import java.util.Map;
import java.lang.Integer;

import org.json.JSONObject;


public class RCTTwilioChatChannels extends ReactContextBaseJavaModule {

    @Override
    public String getName() {
        return "TwilioChatChannels";
    }

    private ReactApplicationContext reactContext;
    private HashMap<String,ChannelListener> channelListeners = new HashMap<String, ChannelListener>();

    public RCTTwilioIPMessagingChannels(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private ChannelListener generateListener(final Channel channel) {
        final String channelSid = channel.getSid();

        ChannelListener listener = new ChannelListener() {
            @Override
            public void onMessageAdd(Message message) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("message", RCTConvert.Message(message));
                sendEvent("ipMessagingClient:channel:messageAdded", map);
            }

            @Override
            public void onMessageChange(Message message) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("message", RCTConvert.Message(message));

                sendEvent("ipMessagingClient:channel:messageChanged", map);
            }

            @Override
            public void onMessageDelete(Message message) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("message", RCTConvert.Message(message));

                sendEvent("ipMessagingClient:channel:messageDeleted", map);
            }

            @Override
            public void onMemberJoin(Member member) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("member", RCTConvert.Member(member));

                sendEvent("ipMessagingClient:channel:memberJoined", map);
            }

            @Override
            public void onMemberChange(Member member) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("member", RCTConvert.Member(member));

                sendEvent("ipMessagingClient:channel:memberChanged", map);
            }

            @Override
            public void onMemberDelete(Member member) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("member", RCTConvert.Member(member));

                sendEvent("ipMessagingClient:channel:memberLeft", map);
            }

            // TODO
            @Override
            public void onAttributesChange(Map<String, String> map) {

            }

            @Override
            public void onTypingStarted(Member member) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("member", RCTConvert.Member(member));

                sendEvent("ipMessagingClient:typingStartedOnChannel", map);
            }

            @Override
            public void onTypingEnded(Member member) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putMap("member", RCTConvert.Member(member));

                sendEvent("ipMessagingClient:typingEndedOnChannel", map);
            }

            @Override
            public void onSynchronizationChange(Channel channel) {
                WritableMap map = Arguments.createMap();
                map.putString("channelSid", channelSid);
                map.putString("status", channel.getSynchronizationStatus().toString());

                sendEvent("ipMessagingClient:channel:synchronizationStatusChanged", map);
            }
        };

        return listener;
    }

    private Channels channels() {
        return RCTTwilioChatClient.getInstance().client.getChannels();
    }

    public static void loadChannelFromSid(String sid, CallbackListener<Channel> callback) {
        channels().getChannel(sid, new CallbackListener<Channel>() {
            @Override
            public void onSuccess(final Channel channel) {
                if (!channelListeners.containsKey(sid)) {
                    ChannelListener listener = generateListener(channel);
                    channel.setListener(listener);
                    channelListeners.put(channel.getSid(), listener);
                }
                callback.onSucess(channel);
            };

            @Override
            public void onError(final ErrorInfo errorInfo){
                callback.onError(errorInfo);
            }

        });
    }

    @ReactMethod
    public void getUserChannels(final Promise promise) {
        channels().getUserChannels(new CallbackListener<Paginator<Channel>>() {
            @Override
            public void onError(final ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("get-user-channels-error", "Error occurred while attempting to getUserChannels.");
            }

            @Override
            public void onSuccess(final Paginator<Channel> channelPaginator) {
                String uuid = RCTTwilioChatPaginator.setPaginator(channelPaginator);
                promise.resolve(RCTConvert.Paginator(channelPaginator, uuid, "Channel"));
            }
        });
    }

    @ReactMethod
    public void getPublicChannels(final Promise promise) {
        channels().getPublicChannels(new CallbackListener<Paginator<ChannelDescriptor>>() {
            @Override
            public void onError(final ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("get-public-channels-error", "Error occurred while attempting to getPublicChannels.");
            }

            @Override
            public void onSuccess(final Paginator<ChannelDescriptor> channelDescriptorPaginator) {
                String uuid = RCTTwilioChatPaginator.setPaginator(channelDescriptorPaginator);
                promise.resolve(RCTConvert.Paginator(channelDescriptorPaginator, uuid, "Channel"));
            }
        });
    }

    @ReactMethod
    public void createChannel(ReadableMap options, final Promise promise) {
        final JSONObject attributes = RCTConvert.readableMapToJson(options.getMap("attributes"));
        final String uniqueName = options.getString("uniqueName");
        String friendlyName = options.getString("friendlyName");
        Channel.ChannelType type = (options.getString("type").compareTo("CHANNEL_TYPE_PRIVATE") == 0) ? Channel.ChannelType.CHANNEL_TYPE_PRIVATE : Channel.ChannelType.CHANNEL_TYPE_PUBLIC;

        channels().channelBuilder()
                .withUniqueName(uniqueName)
                .withFriendlyName(friendlyName)
                .withType(type)
                .withAttributes(attributes)
                .build(new CallbackListener<Channel>() {
                    @Override
                    public void onError(final ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("create-channel-error", "Error occurred while attempting to createChannel.");
                    }

                    @Override
                    public void onCreated(final Channel newChannel) {
                        promise.resolve(RCTConvert.Channel(newChannel));
                    }
                });
    }

    @ReactMethod
    public void getChannel(String sid, Promise promise) {
        RCTTwilioChatChannels.loadChannelFromSid(sid, new ChannelListener<Channel>() {
            @Override
            public void onError(final ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("get-channel-error", "Error occurred while attempting to getChannel.");
            }

            @Override
            public void onCreated(final Channel channel) {
                promise.resolve(RCTConvert.Channel(channel));
            }
        });
    }

    // Channel Instance Methods

    @ReactMethod
    public void synchronize(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("synchronize-error", "Error occurred while attempting to synchronize channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.synchronize(new CallbackListener<Channel>() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("synchronize-error", "Error occurred while attempting to synchronize channel.");
                    }

                    @Override
                    public void onSuccess(Channel channel) {
                        resolve(true);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void setAttributes(String sid, ReadableMap attributes, final Promise promise) {
        JSONObject json = RCTConvert.readableMapToJson(attributes);

        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("set-attributes-error", "Error occurred while attempting to setAttributes on channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.setAttributes(json, new CallbackListener<Channel>() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("set-attributes-error", "Error occurred while attempting to setAttributes on channel.");
                    }

                    @Override
                    public void onSuccess(Channel channel) {
                        resolve(true);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void setFriendlyName(String sid, final String friendlyName, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("set-friendly-name-error", "Error occurred while attempting to setFriendlyName on channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.setFriendlyName(friendlyName, new CallbackListener<Channel>() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("set-friendly-name-error", "Error occurred while attempting to setFriendlyName on channel.");
                    }

                    @Override
                    public void onSuccess(Channel channel) {
                        resolve(true);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void setUniqueName(String sid, final String uniqueName, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("set-unique-name-error", "Error occurred while attempting to setUniqueName on channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.setUniqueName(uniqueName, new CallbackListener<Channel>() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("set-unique-name-error", "Error occurred while attempting to setUniqueName on channel.");
                    }

                    @Override
                    public void onSuccess(Channel channel) {
                        resolve(true);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void join(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("join-error", "Error occurred while attempting to join channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.join(new Constants.StatusListener() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("join-error", "Error occurred while attempting to join channel.");
                    }

                    @Override
                    public void onSuccess() {
                        promise.resolve(true);
                    }
                })
            }
        });
    }

    @ReactMethod
    public void declineInvitation(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("decline-error", "Error occurred while attempting to decline channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.declineInvitation(new Constants.StatusListener() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("decline-error", "Error occurred while attempting to decline channel.");
                    }

                    @Override
                    public void onSuccess() {
                        promise.resolve(true);
                    }
                })
            }
        });
    }

    @ReactMethod
    public void leave(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("leave-error", "Error occurred while attempting to leave channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.leave(new Constants.StatusListener() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("leave-error", "Error occurred while attempting to leave channel.");
                    }

                    @Override
                    public void onSuccess() {
                        promise.resolve(true);
                    }
                })
            }
        });
    }

    @ReactMethod
    public void destroy(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("delete-error", "Error occurred while attempting to delete channel.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.destroy(new Constants.StatusListener() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("delete-error", "Error occurred while attempting to delete channel.");
                    }

                    @Override
                    public void onSuccess() {
                        promise.resolve(true);
                    }
                })
            }
        });
    }

    @ReactMethod
    public void typing(String sid) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onSuccess(Channel channel) {
                channel.typing();
            }
        });
    }

    @ReactMethod
    public void getUnconsumedMessagesCount(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("get-unconsumed-messages-count-error", "Error occurred while attempting to getUnconsumedMessagesCount.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.getUnconsumedMessagesCount(new CallbackListener<Integer>() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("get-unconsumed-messages-count-error", "Error occurred while attempting to getUnconsumedMessagesCount.");
                    }

                    @Override
                    public void onSuccess(Integer count) {
                        promise.resolve(count);
                    }
                })
            }
        });
    }

    @ReactMethod
    public void getMessagesCount(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("get-messages-count-error", "Error occurred while attempting to getMessagesCount.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.getMessagesCount(new CallbackListener<Integer>() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("get-messages-count-error", "Error occurred while attempting to getMessagesCount.");
                    }

                    @Override
                    public void onSuccess(Integer count) {
                        promise.resolve(count);
                    }
                })
            }
        });
    }

    @ReactMethod
    public void getMembersCount(String sid, final Promise promise) {
        loadChannelFromSid(sid, new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("get-members-count-error", "Error occurred while attempting to getMembersCount.");
            }

            @Override
            public void onSuccess(Channel channel) {
                channel.getMembersCount(new CallbackListener<Integer>() {
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                        promise.reject("get-members-count-error", "Error occurred while attempting to getMembersCount.");
                    }

                    @Override
                    public void onSuccess(Integer count) {
                        promise.resolve(count);
                    }
                })
            }
        });
    }


    /*
    @ReactMethod
    public void getMember(String channelSid, String identity, final Promise promise) {
        Member[] members = loadChannelFromSid(channelSid).getMembers().getMembers();
        try {
            for (Member m : members) {
                if (m.getUserInfo().getIdentity() == identity) {
                    promise.resolve(RCTConvert.Member(m));
                    break;
                }
            }
        }
        catch (Exception e) {
            promise.reject("get-member-error","Error occurred while attempting to getMember.");
        }
    }
    */

}