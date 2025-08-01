import SockJS from 'sockjs-client';

import Stomp from 'webstomp-client';
import { Observable } from 'rxjs';
import { Storage } from 'react-jhipster';

import { websocketActivityMessage } from 'app/modules/administration/administration.reducer';
import { getAccount, logoutSession } from 'app/shared/reducers/authentication';

let stompClient: any = null;

let subscriber: any = null;
let connection: Promise<any>;
let connectedPromise: any = null;
let listener: Observable<any>;
let listenerObserver: any;
let alreadyConnectedOnce = false;

const createConnection = (): Promise<any> => new Promise(resolve => (connectedPromise = resolve));

const createListener = (): Observable<any> =>
  new Observable(observer => {
    listenerObserver = observer;
  });

export const sendActivity = (page: string) => {
  if (connection && stompClient) {
    connection
      .then(() => {
        if (stompClient && stompClient.connected) {
          stompClient.send(
            '/topic/activity', // destination
            JSON.stringify({ page }), // body
            {}, // header
          );
        }
      })
      .catch(() => {
        // Silently handle connection errors
      });
  }
};

const subscribe = () => {
  if (connection && stompClient) {
    connection
      .then(() => {
        if (stompClient && stompClient.connected) {
          subscriber = stompClient.subscribe('/topic/tracker', data => {
            try {
              listenerObserver.next(JSON.parse(data.body));
            } catch (error) {
              // Handle JSON parsing errors
              console.warn('Failed to parse websocket message:', error);
            }
          });
        }
      })
      .catch(() => {
        // Silently handle connection errors
      });
  }
};

const connect = () => {
  if (connectedPromise !== null || alreadyConnectedOnce) {
    // the connection is already being established
    return;
  }
  connection = createConnection();
  listener = createListener();

  // building absolute path so that websocket doesn't fail when deploying with a context path
  const loc = window.location;
  const baseElement = document.querySelector('base');
  const baseHref = baseElement ? baseElement.getAttribute('href')?.replace(/\/$/, '') || '' : '';

  const headers = {};
  let url = `//${loc.host}${baseHref}/websocket/tracker`;
  const authToken = Storage.local.get('jhi-authenticationToken') || Storage.session.get('jhi-authenticationToken');
  if (authToken) {
    url += `?access_token=${authToken}`;
  }

  try {
    const socket = new SockJS(url);
    stompClient = Stomp.over(socket, { protocols: ['v12.stomp'] });

    stompClient.connect(
      headers,
      () => {
        connectedPromise('success');
        connectedPromise = null;
        sendActivity(window.location.pathname);
        alreadyConnectedOnce = true;
      },
      (error: any) => {
        // Handle connection errors
        console.warn('Websocket connection failed:', error);
        connectedPromise = null;
      },
    );
  } catch (error) {
    // Handle SockJS creation errors
    console.warn('Failed to create SockJS connection:', error);
    connectedPromise = null;
  }
};

const disconnect = () => {
  if (stompClient !== null) {
    try {
      if (stompClient.connected) {
        stompClient.disconnect();
      }
    } catch (error) {
      // Handle disconnect errors
      console.warn('Error disconnecting websocket:', error);
    } finally {
      stompClient = null;
    }
  }
  alreadyConnectedOnce = false;
};

const receive = () => listener;

const unsubscribe = () => {
  if (subscriber !== null) {
    try {
      subscriber.unsubscribe();
    } catch (error) {
      // Handle unsubscribe errors
      console.warn('Error unsubscribing from websocket:', error);
    } finally {
      subscriber = null;
    }
  }
  listener = createListener();
};

export default store => next => action => {
  if (getAccount.fulfilled.match(action)) {
    connect();
    const isAdmin = action.payload?.data?.authorities?.includes('ROLE_ADMIN') || false;
    if (!alreadyConnectedOnce && isAdmin) {
      subscribe();
      receive().subscribe(activity => {
        return store.dispatch(websocketActivityMessage(activity));
      });
    }
  } else if (getAccount.rejected.match(action) || action.type === logoutSession().type) {
    unsubscribe();
    disconnect();
  }
  return next(action);
};
