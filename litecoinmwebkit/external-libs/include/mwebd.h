/*
 * C interface of libmwebd.so, the MWEB daemon (github.com/ltcmweb/mwebd)
 * built as a shared library by go/build.sh. The functions are implemented
 * in go/mwebd.go; keep the two in step.
 *
 * Strings returned through char* and **err_out are malloc'd and must be
 * released with mweb_free.
 */
#ifndef MWEBD_H
#define MWEBD_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Starts the daemon and its gRPC server on a loopback port, which is
 * returned. chain is "mainnet", "testnet" or "regtest". proxy_url is a SOCKS
 * proxy URL such as "socks5://127.0.0.1:9050" or "" for direct connections.
 * checkpoint_path is a header checkpoint bundle (go/cmd/mkcheckpoint) or ""
 * to sync from genesis. blocks_only != 0 asks peers not to relay unconfirmed
 * transactions. Returns -1 and sets *err_out on failure. While the daemon is
 * running, further calls return the current port.
 */
int mweb_start(const char *data_dir, const char *chain, const char *proxy_url,
               const char *checkpoint_path, int blocks_only, char **err_out);

/* Stops the daemon. */
void mweb_stop(void);

/*
 * Derives the MWEB addresses with indices [from, to) for the account given by
 * its hex-encoded 32-byte scan secret and 33-byte compressed spend public
 * key, newline separated. Returns NULL and sets *err_out on failure.
 */
char *mweb_addresses(const char *scan_secret_hex, const char *spend_pubkey_hex,
                     const char *chain, unsigned int from, unsigned int to,
                     char **err_out);

/* Releases a string returned by this library. */
void mweb_free(char *p);

#ifdef __cplusplus
}
#endif

#endif /* MWEBD_H */
